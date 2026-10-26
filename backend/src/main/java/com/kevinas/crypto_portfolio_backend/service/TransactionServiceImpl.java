package com.kevinas.crypto_portfolio_backend.service;

import com.kevinas.crypto_portfolio_backend.dto.PaginatedTransactionsResponse;
import com.kevinas.crypto_portfolio_backend.dto.TransactionRequest;
import com.kevinas.crypto_portfolio_backend.dto.TransactionResponse;
import com.kevinas.crypto_portfolio_backend.dto.TransactionSummaryResponse;
import com.kevinas.crypto_portfolio_backend.event.PortfolioStateChangedEvent;
import com.kevinas.crypto_portfolio_backend.exception.InsufficientHoldingsException;
import com.kevinas.crypto_portfolio_backend.exception.TransactionConflictException;
import com.kevinas.crypto_portfolio_backend.exception.TransactionNotFoundException;
import com.kevinas.crypto_portfolio_backend.model.Coin;
import com.kevinas.crypto_portfolio_backend.model.Holding;
import com.kevinas.crypto_portfolio_backend.model.Transaction;
import com.kevinas.crypto_portfolio_backend.model.TransactionType;
import com.kevinas.crypto_portfolio_backend.model.User;
import com.kevinas.crypto_portfolio_backend.repository.CoinRepository;
import com.kevinas.crypto_portfolio_backend.repository.HoldingRepository;
import com.kevinas.crypto_portfolio_backend.repository.PortfolioSnapshotRepository;
import com.kevinas.crypto_portfolio_backend.repository.TransactionRepository;
import com.kevinas.crypto_portfolio_backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class TransactionServiceImpl implements TransactionService {

    private final TransactionRepository transactionRepository;
    private final HoldingRepository holdingRepository;
    private final CoinRepository coinRepository;
    private final UserRepository userRepository;
    private final PortfolioSnapshotRepository portfolioSnapshotRepository;
    private final ApplicationEventPublisher eventPublisher;

    @Override
    @Transactional
    public TransactionResponse createTransaction(TransactionRequest request) {
        User user = getCurrentUserForUpdate();
        Coin coin = resolveCoin(request);

        Transaction transaction = buildTransaction(
                user,
                coin,
                request,
                transactionRepository.findMaximumLedgerSequence(user) + 1,
                null
        );

        Transaction saved = transactionRepository.saveAndFlush(transaction);
        replayActiveLedger(user, "Insufficient holdings to complete sell transaction");
        publishPortfolioChange(saved.getId(), user.getId());
        return toResponse(saved);
    }

    @Override
    @Transactional
    public TransactionResponse updateTransaction(Long transactionId, TransactionRequest request) {
        User user = getCurrentUserForUpdate();
        Transaction currentRevision = getOwnedTransaction(transactionId, user);
        requireActiveRevision(currentRevision);

        Instant historicalCutoff = currentRevision.getCreatedAt();
        currentRevision.setVoidedAt(Instant.now());
        transactionRepository.saveAndFlush(currentRevision);

        Transaction replacement = buildTransaction(
                user,
                resolveCoin(request),
                request,
                currentRevision.getLedgerSequence(),
                historicalCutoff
        );
        Transaction savedReplacement = transactionRepository.saveAndFlush(replacement);

        currentRevision.setReplacementTransaction(savedReplacement);
        transactionRepository.save(currentRevision);

        replayActiveLedger(user, "Transaction change would make a historical sell exceed available holdings");
        invalidateSnapshots(user, historicalCutoff);
        publishPortfolioChange(savedReplacement.getId(), user.getId());
        return toResponse(savedReplacement);
    }

    @Override
    @Transactional
    public void deleteTransaction(Long transactionId) {
        User user = getCurrentUserForUpdate();
        Transaction currentRevision = getOwnedTransaction(transactionId, user);
        requireActiveRevision(currentRevision);

        Instant historicalCutoff = currentRevision.getCreatedAt();
        currentRevision.setVoidedAt(Instant.now());
        transactionRepository.saveAndFlush(currentRevision);

        replayActiveLedger(user, "Transaction change would make a historical sell exceed available holdings");
        invalidateSnapshots(user, historicalCutoff);
        publishPortfolioChange(currentRevision.getId(), user.getId());
    }

    @Override
    @Transactional(readOnly = true, isolation = Isolation.REPEATABLE_READ)
    public List<TransactionResponse> getTransactionsForCurrentUser() {
        User user = getCurrentUser();
        Page<Transaction> page = transactionRepository.findByUserAndVoidedAtIsNull(user,
                PageRequest.of(0, 10000, Sort.by(Sort.Direction.DESC, "ledgerSequence")));
        if (page.hasNext()) throw new IllegalArgumentException("Use paginated history for more than 10,000 transactions");
        return page.getContent().stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    public PaginatedTransactionsResponse getTransactionsForCurrentUserPaginated(int pageNumber, int pageSize) {
        if (pageNumber < 0 || pageSize < 1 || pageSize > 100) {
            throw new IllegalArgumentException("Page must be non-negative and size must be between 1 and 100");
        }
        User user = getCurrentUser();

        Pageable pageable = PageRequest.of(pageNumber, pageSize, Sort.by(Sort.Direction.DESC, "ledgerSequence"));
        Page<Transaction> page = transactionRepository.findByUserAndVoidedAtIsNull(user, pageable);

        List<TransactionResponse> content = page.getContent().stream()
                .map(this::toResponse)
                .toList();

        return new PaginatedTransactionsResponse(
                content,
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages(),
                page.hasNext(),
                page.hasPrevious()
        );
    }

    @Override
    public TransactionSummaryResponse getTransactionSummary() {
        User user = getCurrentUser();
        var totals = transactionRepository.aggregateActiveTotals(user);
        return new TransactionSummaryResponse(
                scaleMoney(totals.totalBuyVolumeUsd()),
                scaleMoney(totals.totalSellVolumeUsd()),
                scaleMoney(totals.totalRealisedProfitUsd())
        );
    }

    private void replayActiveLedger(User user, String insufficientHoldingsMessage) {
        List<Transaction> transactions = transactionRepository
                .findByUserAndVoidedAtIsNullOrderByLedgerSequenceAsc(user);
        Map<Long, Position> positions = new LinkedHashMap<>();

        for (Transaction transaction : transactions) {
            transaction.setQuantity(scaleQuantity(transaction.getQuantity()));
            transaction.setTotalValueUsd(scaleMoney(
                    transaction.getQuantity().multiply(transaction.getPriceUsd())
            ));

            Long coinId = transaction.getCoin().getId();
            Position position = positions.get(coinId);

            if (transaction.getType() == TransactionType.BUY) {
                positions.put(coinId, applyBuy(transaction, position));
                transaction.setRealisedProfitUsd(scaleMoney(BigDecimal.ZERO));
            } else if (transaction.getType() == TransactionType.SELL) {
                positions.compute(
                        coinId,
                        (ignored, currentPosition) -> applySell(
                                transaction,
                                currentPosition,
                                insufficientHoldingsMessage
                        )
                );
            } else {
                throw new IllegalArgumentException("Unsupported transaction type");
            }
        }

        transactionRepository.saveAll(transactions);
        rebuildHoldings(user, positions);
    }

    private Position applyBuy(Transaction transaction, Position position) {
        if (position == null) {
            return new Position(
                    transaction.getCoin(),
                    scaleQuantity(transaction.getQuantity()),
                    transaction.getQuantity().multiply(transaction.getPriceUsd()),
                    transaction.getAssetName()
            );
        }

        BigDecimal updatedQuantity = position.quantity.add(transaction.getQuantity());
        if (updatedQuantity.precision() - updatedQuantity.scale() > 11) {
            throw new IllegalArgumentException("Total asset quantity exceeds supported limit");
        }
        BigDecimal updatedCost = position.costBasisUsd
                .add(transaction.getQuantity().multiply(transaction.getPriceUsd()));
        requireMoneyRange(updatedCost);

        return new Position(
                transaction.getCoin(),
                scaleQuantity(updatedQuantity),
                updatedCost,
                transaction.getAssetName()
        );
    }

    private Position applySell(Transaction transaction, Position position, String insufficientHoldingsMessage) {
        if (position == null || position.quantity.compareTo(transaction.getQuantity()) < 0) {
            throw new InsufficientHoldingsException(insufficientHoldingsMessage);
        }

        BigDecimal allocatedCost = position.quantity.compareTo(transaction.getQuantity()) == 0
                ? position.costBasisUsd
                : position.costBasisUsd.multiply(transaction.getQuantity())
                    .divide(position.quantity, 20, RoundingMode.HALF_UP);
        BigDecimal realisedProfit = transaction.getPriceUsd().multiply(transaction.getQuantity())
                .subtract(allocatedCost);
        transaction.setRealisedProfitUsd(realisedProfit);

        BigDecimal remainingQuantity = scaleQuantity(position.quantity.subtract(transaction.getQuantity()));
        return remainingQuantity.compareTo(BigDecimal.ZERO) == 0
                ? null
                : new Position(transaction.getCoin(), remainingQuantity,
                        position.costBasisUsd.subtract(allocatedCost), position.assetName);
    }

    private void rebuildHoldings(User user, Map<Long, Position> positions) {
        requireMoneyRange(positions.values().stream().map(Position::costBasisUsd).reduce(BigDecimal.ZERO, BigDecimal::add));
        holdingRepository.deleteAllByUser(user);
        holdingRepository.flush();

        List<Holding> rebuiltHoldings = positions.values().stream()
                .map(position -> Holding.builder()
                        .user(user)
                        .coin(position.coin)
                        .quantity(position.quantity)
                        .averageBuyPriceUsd(position.costBasisUsd.divide(position.quantity, 12, RoundingMode.HALF_UP))
                        .costBasisUsd(position.costBasisUsd)
                        .assetName(position.assetName)
                        .build())
                .toList();

        holdingRepository.saveAll(rebuiltHoldings);
    }

    private Transaction buildTransaction(
            User user,
            Coin coin,
            TransactionRequest request,
            Long ledgerSequence,
            Instant createdAt
    ) {
        requireMoneyRange(request.quantity().multiply(request.priceUsd()));
        return Transaction.builder()
                .user(user)
                .coin(coin)
                .type(request.type())
                .quantity(scaleQuantity(request.quantity()))
                .priceUsd(request.priceUsd())
                .assetName(com.kevinas.crypto_portfolio_backend.util.CoinGeckoSymbolMapper.getSupportedCoins()
                        .stream().filter(item -> item.symbol().equals(request.symbol()))
                        .map(item -> item.name()).findFirst().orElse(request.name()))
                .totalValueUsd(scaleMoney(request.quantity().multiply(request.priceUsd())))
                .realisedProfitUsd(scaleMoney(BigDecimal.ZERO))
                .ledgerSequence(ledgerSequence)
                .createdAt(createdAt)
                .build();
    }

    private Coin resolveCoin(TransactionRequest request) {
        return coinRepository.findBySymbolIgnoreCase(request.symbol())
                .orElseGet(() -> {
                    coinRepository.insertIfAbsent(request.symbol());
                    return coinRepository.findBySymbolIgnoreCase(request.symbol())
                            .orElseThrow(() -> new IllegalStateException("Asset catalog insertion failed"));
                });
    }

    private Transaction getOwnedTransaction(Long transactionId, User user) {
        return transactionRepository.findByIdAndUser(transactionId, user)
                .orElseThrow(() -> new TransactionNotFoundException("Transaction not found"));
    }

    private void requireActiveRevision(Transaction transaction) {
        if (transaction.getVoidedAt() != null) {
            throw new TransactionConflictException(
                    "Transaction has already been edited or deleted; refresh before trying again"
            );
        }
    }

    private void invalidateSnapshots(User user, Instant historicalCutoff) {
        portfolioSnapshotRepository.deleteByUserAndSnapshotAtGreaterThanEqual(user, historicalCutoff);
    }

    private void publishPortfolioChange(Long transactionId, Long userId) {
        eventPublisher.publishEvent(new PortfolioStateChangedEvent(transactionId, userId));
    }

    private TransactionResponse toResponse(Transaction transaction) {
        return new TransactionResponse(
                transaction.getId(),
                transaction.getCoin().getSymbol(),
                transaction.getAssetName() == null ? transaction.getCoin().getName() : transaction.getAssetName(),
                transaction.getType(),
                scaleQuantity(transaction.getQuantity()),
                transaction.getPriceUsd(),
                scaleMoney(transaction.getTotalValueUsd()),
                scaleMoney(transaction.getRealisedProfitUsd()),
                transaction.getCreatedAt()
        );
    }

    private User getCurrentUser() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalStateException("Authenticated user not found"));
    }

    private User getCurrentUserForUpdate() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByEmailForUpdate(email)
                .orElseThrow(() -> new IllegalStateException("Authenticated user not found"));
    }

    private BigDecimal scaleMoney(BigDecimal value) {
        return value.setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal scaleQuantity(BigDecimal value) {
        try {
            return value.setScale(8, RoundingMode.UNNECESSARY);
        } catch (ArithmeticException ex) {
            throw new IllegalArgumentException("Quantity supports at most 8 decimal places");
        }
    }

    private void requireMoneyRange(BigDecimal value) {
        if (value.abs().compareTo(new BigDecimal("99999999999999999.99")) > 0) {
            throw new IllegalArgumentException("Transaction or cost basis exceeds supported USD limit");
        }
    }

    private record Position(Coin coin, BigDecimal quantity, BigDecimal costBasisUsd, String assetName) {
    }
}
