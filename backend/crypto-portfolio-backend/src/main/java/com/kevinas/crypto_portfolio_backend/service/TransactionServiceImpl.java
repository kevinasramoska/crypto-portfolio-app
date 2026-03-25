package com.kevinas.crypto_portfolio_backend.service;

import com.kevinas.crypto_portfolio_backend.dto.TransactionRequest;
import com.kevinas.crypto_portfolio_backend.dto.TransactionResponse;
import com.kevinas.crypto_portfolio_backend.dto.TransactionSummaryResponse;
import com.kevinas.crypto_portfolio_backend.exception.InsufficientHoldingsException;
import com.kevinas.crypto_portfolio_backend.model.Coin;
import com.kevinas.crypto_portfolio_backend.model.Holding;
import com.kevinas.crypto_portfolio_backend.model.Transaction;
import com.kevinas.crypto_portfolio_backend.model.TransactionType;
import com.kevinas.crypto_portfolio_backend.model.User;
import com.kevinas.crypto_portfolio_backend.repository.CoinRepository;
import com.kevinas.crypto_portfolio_backend.repository.HoldingRepository;
import com.kevinas.crypto_portfolio_backend.repository.TransactionRepository;
import com.kevinas.crypto_portfolio_backend.repository.UserRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

@Service
@RequiredArgsConstructor
public class TransactionServiceImpl implements TransactionService {

    private final TransactionRepository transactionRepository;
    private final HoldingRepository holdingRepository;
    private final CoinRepository coinRepository;
    private final UserRepository userRepository;
    private final PortfolioService portfolioService;

    @Override
    @Transactional
    public TransactionResponse createTransaction(TransactionRequest request) {
        User user = getCurrentUser();

        Coin coin = coinRepository.findBySymbolIgnoreCase(request.symbol())
                .orElseGet(() -> coinRepository.save(
                        Coin.builder()
                                .symbol(request.symbol().toUpperCase())
                                .name(request.name())
                                .build()
                ));

        Holding holding = holdingRepository.findByUserAndCoin_SymbolIgnoreCase(user, request.symbol())
                .orElse(null);

        BigDecimal totalValueUsd = request.quantity().multiply(request.priceUsd());
        BigDecimal realisedProfitUsd = BigDecimal.ZERO;

        if (request.type() == TransactionType.BUY) {
            holding = handleBuy(user, coin, holding, request.quantity(), request.priceUsd());
        } else if (request.type() == TransactionType.SELL) {
            validateSellTransaction(holding, request.quantity());
            realisedProfitUsd = handleSell(holding, request.quantity(), request.priceUsd());
        } else {
            throw new IllegalArgumentException("Unsupported transaction type");
        }

        Transaction transaction = Transaction.builder()
                .user(user)
                .coin(coin)
                .type(request.type())
                .quantity(scaleQuantity(request.quantity()))
                .priceUsd(scaleMoney(request.priceUsd()))
                .totalValueUsd(scaleMoney(totalValueUsd))
                .realisedProfitUsd(scaleMoney(realisedProfitUsd))
                .build();

        Transaction saved = transactionRepository.save(transaction);
        portfolioService.createSnapshotForCurrentUser();

        return toResponse(saved);
    }

    @Override
    public List<TransactionResponse> getTransactionsForCurrentUser() {
        User user = getCurrentUser();

        return transactionRepository.findByUserOrderByCreatedAtDesc(user).stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    public TransactionSummaryResponse getTransactionSummary() {
        User user = getCurrentUser();
        List<Transaction> transactions = transactionRepository.findByUserOrderByCreatedAtDesc(user);

        BigDecimal totalBuyVolume = transactions.stream()
                .filter(t -> t.getType() == TransactionType.BUY)
                .map(Transaction::getTotalValueUsd)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalSellVolume = transactions.stream()
                .filter(t -> t.getType() == TransactionType.SELL)
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

    private Holding handleBuy(User user, Coin coin, Holding holding, BigDecimal buyQuantity, BigDecimal buyPriceUsd) {
        if (holding == null) {
            holding = Holding.builder()
                    .user(user)
                    .coin(coin)
                    .quantity(scaleQuantity(buyQuantity))
                    .averageBuyPriceUsd(scaleMoney(buyPriceUsd))
                    .build();

            return holdingRepository.save(holding);
        }

        BigDecimal existingQuantity = holding.getQuantity();
        BigDecimal existingAvgPrice = holding.getAverageBuyPriceUsd();

        BigDecimal existingCostBasis = existingQuantity.multiply(existingAvgPrice);
        BigDecimal newCostBasis = buyQuantity.multiply(buyPriceUsd);

        BigDecimal updatedQuantity = existingQuantity.add(buyQuantity);
        BigDecimal updatedAveragePrice = existingCostBasis.add(newCostBasis)
                .divide(updatedQuantity, 8, RoundingMode.HALF_UP);

        holding.setQuantity(scaleQuantity(updatedQuantity));
        holding.setAverageBuyPriceUsd(scaleMoney(updatedAveragePrice));

        return holdingRepository.save(holding);
    }

    private BigDecimal handleSell(Holding holding, BigDecimal sellQuantity, BigDecimal sellPriceUsd) {
        BigDecimal averageBuyPrice = holding.getAverageBuyPriceUsd();
        BigDecimal realisedProfit = sellPriceUsd.subtract(averageBuyPrice).multiply(sellQuantity);

        BigDecimal remainingQuantity = holding.getQuantity().subtract(sellQuantity);

        if (remainingQuantity.compareTo(BigDecimal.ZERO) == 0) {
            holdingRepository.delete(holding);
        } else {
            holding.setQuantity(scaleQuantity(remainingQuantity));
            holdingRepository.save(holding);
        }

        return realisedProfit;
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

    private BigDecimal scaleMoney(BigDecimal value) {
        return value.setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal scaleQuantity(BigDecimal value) {
        return value.setScale(8, RoundingMode.HALF_UP);
    }

    private void validateSellTransaction(Holding holding, BigDecimal sellQuantity) {
        // Prevent overselling: ensure user has sufficient holdings before proceeding
        if (holding == null || holding.getQuantity().compareTo(sellQuantity) < 0) {
            throw new InsufficientHoldingsException("Insufficient holdings to complete sell transaction");
        }
    }
}
