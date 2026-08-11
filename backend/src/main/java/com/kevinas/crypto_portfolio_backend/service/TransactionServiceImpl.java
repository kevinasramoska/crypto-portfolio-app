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
    public List<TransactionResponse> getTransactionsForCurrentUser() {
        User user = getCurrentUser();

        return transactionRepository.findByUserAndVoidedAtIsNullOrderByLedgerSequenceDesc(user).stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    public PaginatedTransactionsResponse getTransactionsForCurrentUserPaginated(int pageNumber, int pageSize) {
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
        List<Transaction> transactions = transactionRepository
                .findByUserAndVoidedAtIsNullOrderByLedgerSequenceDesc(user);

        BigDecimal totalBuyVolume = transactions.stream()
                .filter(transaction -> transaction.getType() == TransactionType.BUY)
                .map(Transaction::getTotalValueUsd)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalSellVolume = transactions.stream()
                .filter(transaction -> transaction.getType() == TransactionType.SELL)
                .map(Transaction::getTotalValueUsd)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalRealisedProfit = transactions.stream()
                .map(Transaction::getRealisedProfitUsd)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return new TransactionSummaryResponse(
                scaleMoney(totalBuyVolume),
                scaleMoney(totalSellVolume),
                scaleMoney(totalRealisedProfit)
        );
    }

    private void replayActiveLedger(User user, String insufficientHoldingsMessage) {
        List<Transaction> transactions = transactionRepository
                .findByUserAndVoidedAtIsNullOrderByLedgerSequenceAsc(user);
        Map<Long, Position> positions = new LinkedHashMap<>();

        for (Transaction transaction : transactions) {
            transaction.setQuantity(scaleQuantity(transaction.getQuantity()));
            transaction.setPriceUsd(scaleMoney(transaction.getPriceUsd()));
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
                    scaleMoney(transaction.getPriceUsd())
            );
        }

        BigDecimal updatedQuantity = position.quantity.add(transaction.getQuantity());
        BigDecimal updatedAveragePrice = position.quantity.multiply(position.averageBuyPriceUsd)
                .add(transaction.getQuantity().multiply(transaction.getPriceUsd()))
                .divide(updatedQuantity, 8, RoundingMode.HALF_UP);

        return new Position(
                transaction.getCoin(),
                scaleQuantity(updatedQuantity),
                scaleMoney(updatedAveragePrice)
        );
    }

    private Position applySell(Transaction transaction, Position position, String insufficientHoldingsMessage) {
        if (position == null || position.quantity.compareTo(transaction.getQuantity()) < 0) {
            throw new InsufficientHoldingsException(insufficientHoldingsMessage);
        }

        BigDecimal realisedProfit = transaction.getPriceUsd()
                .subtract(position.averageBuyPriceUsd)
                .multiply(transaction.getQuantity());
        transaction.setRealisedProfitUsd(scaleMoney(realisedProfit));

        BigDecimal remainingQuantity = scaleQuantity(position.quantity.subtract(transaction.getQuantity()));
        return remainingQuantity.compareTo(BigDecimal.ZERO) == 0
                ? null
                : new Position(transaction.getCoin(), remainingQuantity, position.averageBuyPriceUsd);
    }

    private void rebuildHoldings(User user, Map<Long, Position> positions) {
        holdingRepository.deleteAllByUser(user);
        holdingRepository.flush();

        List<Holding> rebuiltHoldings = positions.values().stream()
                .map(position -> Holding.builder()
                        .user(user)
                        .coin(position.coin)
                        .quantity(position.quantity)
                        .averageBuyPriceUsd(position.averageBuyPriceUsd)
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
        return Transaction.builder()
                .user(user)
                .coin(coin)
                .type(request.type())
                .quantity(scaleQuantity(request.quantity()))
                .priceUsd(scaleMoney(request.priceUsd()))
                .totalValueUsd(scaleMoney(request.quantity().multiply(request.priceUsd())))
                .realisedProfitUsd(scaleMoney(BigDecimal.ZERO))
                .ledgerSequence(ledgerSequence)
                .createdAt(createdAt)
                .build();
    }

    private Coin resolveCoin(TransactionRequest request) {
        return coinRepository.findBySymbolIgnoreCase(request.symbol())
                .orElseGet(() -> coinRepository.save(
                        Coin.builder()
                                .symbol(request.symbol().toUpperCase())
                                .name(request.name())
                                .build()
                ));
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
                transaction.getCoin().getName(),
                transaction.getType(),
                scaleQuantity(transaction.getQuantity()),
                scaleMoney(transaction.getPriceUsd()),
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
        return value.setScale(8, RoundingMode.HALF_UP);
    }

    private record Position(Coin coin, BigDecimal quantity, BigDecimal averageBuyPriceUsd) {
    }
}
