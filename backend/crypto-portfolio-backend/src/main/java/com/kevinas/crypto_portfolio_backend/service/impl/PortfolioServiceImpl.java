package com.kevinas.crypto_portfolio_backend.service.impl;

import com.kevinas.crypto_portfolio_backend.dto.HoldingResponse;
import com.kevinas.crypto_portfolio_backend.dto.PortfolioHoldingSummaryResponse;
import com.kevinas.crypto_portfolio_backend.dto.PortfolioSummaryResponse;
import com.kevinas.crypto_portfolio_backend.model.Holding;
import com.kevinas.crypto_portfolio_backend.model.Transaction;
import com.kevinas.crypto_portfolio_backend.model.TransactionType;
import com.kevinas.crypto_portfolio_backend.model.User;
import com.kevinas.crypto_portfolio_backend.repository.HoldingRepository;
import com.kevinas.crypto_portfolio_backend.repository.TransactionRepository;
import com.kevinas.crypto_portfolio_backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PortfolioServiceImpl implements com.kevinas.crypto_portfolio_backend.service.PortfolioService {

    private static final int USD_SCALE = 2;
    private static final int QTY_SCALE = 8;

    private final HoldingRepository holdingRepository;
    private final TransactionRepository transactionRepository;
    private final UserRepository userRepository;
    private final com.kevinas.crypto_portfolio_backend.service.MarketDataService marketDataService;

    @Override
    public List<HoldingResponse> getUserHoldings() {
        User user = getAuthenticatedUser();

        return holdingRepository.findByUser(user).stream()
                .map(holding -> {
                    BigDecimal currentPriceUsd = safePriceLookup(holding.getCoin().getSymbol());
                    BigDecimal investedValueUsd = money(
                            holding.getQuantity().multiply(holding.getAverageBuyPriceUsd())
                    );
                    BigDecimal currentValueUsd = money(
                            holding.getQuantity().multiply(currentPriceUsd)
                    );
                    BigDecimal profitLossUsd = money(
                            currentValueUsd.subtract(investedValueUsd)
                    );

                    return new HoldingResponse(
                            holding.getId(),
                            holding.getCoin().getSymbol(),
                            holding.getCoin().getName(),
                            scaleQty(holding.getQuantity()),
                            money(holding.getAverageBuyPriceUsd()),
                            money(currentPriceUsd),
                            investedValueUsd,
                            currentValueUsd,
                            profitLossUsd
                    );
                })
                .collect(Collectors.toList());
    }

    @Override
    public PortfolioSummaryResponse getCurrentUserPortfolioSummary() {
        return computePortfolioSummary(getAuthenticatedUser());
    }

    @Override
    public PortfolioSummaryResponse getPortfolioSummaryForUser(User user) {
        return computePortfolioSummary(user);
    }

    private PortfolioSummaryResponse computePortfolioSummary(User user) {
        // Fetch all transactions sorted by creation time ascending
        List<Transaction> transactions = transactionRepository.findByUserOrderByCreatedAtAsc(user);

        // Group transactions by coin
        Map<Long, List<Transaction>> transactionsByCoin = transactions.stream()
                .collect(Collectors.groupingBy(t -> t.getCoin().getId()));

        // Compute holdings from transactions
        List<PortfolioHoldingSummaryResponse> holdingSummaries = new ArrayList<>();
        BigDecimal totalInvestedUsd = BigDecimal.ZERO;
        BigDecimal totalCurrentValueUsd = BigDecimal.ZERO;
        BigDecimal totalUnrealisedProfitLossUsd = BigDecimal.ZERO;

        for (Map.Entry<Long, List<Transaction>> entry : transactionsByCoin.entrySet()) {
            List<Transaction> coinTransactions = entry.getValue();
            com.kevinas.crypto_portfolio_backend.model.Coin coin = coinTransactions.get(0).getCoin(); // All have same coin

            // Process transactions to compute current holding
            BigDecimal quantity = BigDecimal.ZERO;
            BigDecimal costBasis = BigDecimal.ZERO;

            for (Transaction tx : coinTransactions) {
                if (tx.getType() == TransactionType.BUY) {
                    quantity = quantity.add(tx.getQuantity());
                    costBasis = costBasis.add(tx.getQuantity().multiply(tx.getPriceUsd()));
                } else if (tx.getType() == TransactionType.SELL) {
                    if (quantity.compareTo(tx.getQuantity()) >= 0) {
                        // Reduce cost basis proportionally
                        BigDecimal proportion = tx.getQuantity().divide(quantity, 8, RoundingMode.HALF_UP);
                        costBasis = costBasis.subtract(costBasis.multiply(proportion));
                        quantity = quantity.subtract(tx.getQuantity());
                    } // Else, skip invalid sell (though validation should prevent)
                }
            }

            // Skip zero or negative quantity holdings
            if (quantity.compareTo(BigDecimal.ZERO) <= 0) {
                continue;
            }

            BigDecimal averageBuyPriceUsd = costBasis.divide(quantity, 8, RoundingMode.HALF_UP);
            BigDecimal currentPriceUsd = safePriceLookup(coin.getSymbol());
            BigDecimal investedValueUsd = money(quantity.multiply(averageBuyPriceUsd));
            BigDecimal currentValueUsd = money(quantity.multiply(currentPriceUsd));
            BigDecimal unrealisedProfitLossUsd = money(currentValueUsd.subtract(investedValueUsd));

            PortfolioHoldingSummaryResponse summary = PortfolioHoldingSummaryResponse.builder()
                    .symbol(coin.getSymbol())
                    .name(coin.getName())
                    .quantity(scaleQty(quantity))
                    .averageBuyPriceUsd(money(averageBuyPriceUsd))
                    .currentPriceUsd(money(currentPriceUsd))
                    .investedValueUsd(investedValueUsd)
                    .currentValueUsd(currentValueUsd)
                    .unrealisedProfitLossUsd(unrealisedProfitLossUsd)
                    .build();

            holdingSummaries.add(summary);

            totalInvestedUsd = totalInvestedUsd.add(investedValueUsd);
            totalCurrentValueUsd = totalCurrentValueUsd.add(currentValueUsd);
            totalUnrealisedProfitLossUsd = totalUnrealisedProfitLossUsd.add(unrealisedProfitLossUsd);
        }

        // Compute total realized profit/loss from all SELL transactions
        BigDecimal totalRealisedProfitLossUsd = transactions.stream()
                .filter(tx -> tx.getType() == TransactionType.SELL)
                .map(Transaction::getRealisedProfitUsd)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        totalInvestedUsd = money(totalInvestedUsd);
        totalCurrentValueUsd = money(totalCurrentValueUsd);
        totalUnrealisedProfitLossUsd = money(totalUnrealisedProfitLossUsd);
        totalRealisedProfitLossUsd = money(totalRealisedProfitLossUsd);

        BigDecimal totalProfitLossUsd = money(
                totalRealisedProfitLossUsd.add(totalUnrealisedProfitLossUsd)
        );

        return PortfolioSummaryResponse.builder()
                .totalInvestedUsd(totalInvestedUsd)
                .totalCurrentValueUsd(totalCurrentValueUsd)
                .totalUnrealisedProfitLossUsd(totalUnrealisedProfitLossUsd)
                .totalRealisedProfitLossUsd(totalRealisedProfitLossUsd)
                .totalProfitLossUsd(totalProfitLossUsd)
                .holdings(holdingSummaries)
                .build();
    }

    private User getAuthenticatedUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String username = authentication.getName();
        return userRepository.findByEmail(username)
                .orElseThrow(() -> new RuntimeException("User not found: " + username));
    }

    private BigDecimal safePriceLookup(String symbol) {
        BigDecimal price = marketDataService.getCurrentPrice(symbol);
        if (price == null) {
            return BigDecimal.ZERO.setScale(USD_SCALE, RoundingMode.HALF_UP);
        }
        return money(price);
    }

    private BigDecimal money(BigDecimal value) {
        return value.setScale(USD_SCALE, RoundingMode.HALF_UP);
    }

    private BigDecimal scaleQty(BigDecimal quantity) {
        return quantity.setScale(QTY_SCALE, RoundingMode.HALF_UP);
    }
}
