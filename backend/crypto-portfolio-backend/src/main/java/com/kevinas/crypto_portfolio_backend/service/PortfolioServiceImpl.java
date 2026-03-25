package com.kevinas.crypto_portfolio_backend.service;

import com.kevinas.crypto_portfolio_backend.dto.HoldingResponse;
import com.kevinas.crypto_portfolio_backend.dto.PortfolioHoldingSummaryResponse;
import com.kevinas.crypto_portfolio_backend.dto.PortfolioPerformanceHistoryResponse;
import com.kevinas.crypto_portfolio_backend.dto.PortfolioPerformancePointResponse;
import com.kevinas.crypto_portfolio_backend.dto.PortfolioSummaryResponse;
import com.kevinas.crypto_portfolio_backend.model.Holding;
import com.kevinas.crypto_portfolio_backend.model.PortfolioSnapshot;
import com.kevinas.crypto_portfolio_backend.model.Transaction;
import com.kevinas.crypto_portfolio_backend.model.TransactionType;
import com.kevinas.crypto_portfolio_backend.model.User;
import com.kevinas.crypto_portfolio_backend.repository.HoldingRepository;
import com.kevinas.crypto_portfolio_backend.repository.PortfolioSnapshotRepository;
import com.kevinas.crypto_portfolio_backend.repository.TransactionRepository;
import com.kevinas.crypto_portfolio_backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PortfolioServiceImpl implements PortfolioService {

    private static final int USD_SCALE = 2;
    private static final int QTY_SCALE = 8;

    private final HoldingRepository holdingRepository;
    private final TransactionRepository transactionRepository;
    private final UserRepository userRepository;
    private final MarketDataService marketDataService;
    private final PortfolioSnapshotRepository portfolioSnapshotRepository;

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
                .toList();
    }

    @Override
    public PortfolioSummaryResponse getCurrentUserPortfolioSummary() {
        User user = getAuthenticatedUser();
        return computePortfolioSummary(user);
    }


    @Override
    public void createSnapshotForCurrentUser() {
        User user = getAuthenticatedUser();
        PortfolioSummaryResponse summary = computePortfolioSummary(user);
        saveSnapshot(user, summary);
    }

    @Override
    public PortfolioPerformanceHistoryResponse getPortfolioPerformanceHistory(String range) {
        User user = getAuthenticatedUser();
        int days = parseRangeDays(range);
        Instant from = Instant.now().minus(days, ChronoUnit.DAYS);

        List<PortfolioPerformancePointResponse> history = portfolioSnapshotRepository
                .findByUserAndSnapshotAtGreaterThanEqualOrderBySnapshotAtAsc(user, from)
                .stream()
                .map(snapshot -> new PortfolioPerformancePointResponse(
                        snapshot.getSnapshotAt(),
                        money(snapshot.getTotalInvestedUsd()),
                        money(snapshot.getTotalCurrentValueUsd()),
                        money(snapshot.getTotalProfitLossUsd())
                ))
                .toList();

        return new PortfolioPerformanceHistoryResponse(range.toLowerCase(), history);
    }

    private PortfolioSummaryResponse computePortfolioSummary(User user) {
        List<Transaction> transactions = transactionRepository.findByUserOrderByCreatedAtAsc(user);

        Map<Long, List<Transaction>> transactionsByCoin = transactions.stream()
                .collect(Collectors.groupingBy(t -> t.getCoin().getId()));

        List<PortfolioHoldingSummaryResponse> holdingSummaries = new ArrayList<>();
        BigDecimal totalInvestedUsd = BigDecimal.ZERO;
        BigDecimal totalCurrentValueUsd = BigDecimal.ZERO;
        BigDecimal totalUnrealisedProfitLossUsd = BigDecimal.ZERO;

        for (Map.Entry<Long, List<Transaction>> entry : transactionsByCoin.entrySet()) {
            List<Transaction> coinTransactions = entry.getValue();
            com.kevinas.crypto_portfolio_backend.model.Coin coin = coinTransactions.get(0).getCoin();

            BigDecimal quantity = BigDecimal.ZERO;
            BigDecimal costBasis = BigDecimal.ZERO;

            for (Transaction tx : coinTransactions) {
                if (tx.getType() == TransactionType.BUY) {
                    quantity = quantity.add(tx.getQuantity());
                    costBasis = costBasis.add(tx.getQuantity().multiply(tx.getPriceUsd()));
                } else if (tx.getType() == TransactionType.SELL) {
                    if (quantity.compareTo(tx.getQuantity()) >= 0) {
                        BigDecimal proportion = tx.getQuantity().divide(quantity, 8, RoundingMode.HALF_UP);
                        costBasis = costBasis.subtract(costBasis.multiply(proportion));
                        quantity = quantity.subtract(tx.getQuantity());
                    }
                }
            }

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

    private void saveSnapshot(User user, PortfolioSummaryResponse summary) {
        PortfolioSnapshot snapshot = PortfolioSnapshot.builder()
                .user(user)
                .totalInvestedUsd(money(summary.getTotalInvestedUsd()))
                .totalCurrentValueUsd(money(summary.getTotalCurrentValueUsd()))
                .totalProfitLossUsd(money(summary.getTotalProfitLossUsd()))
                .build();

        portfolioSnapshotRepository.save(snapshot);
    }

    private int parseRangeDays(String range) {
        if (range == null) {
            throw new IllegalArgumentException("Range query parameter is required");
        }

        return switch (range.toLowerCase()) {
            case "7d" -> 7;
            case "30d" -> 30;
            case "90d" -> 90;
            default -> throw new IllegalArgumentException("Unsupported range. Use 7d, 30d, or 90d");
        };
    }

    private User getAuthenticatedUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String email = authentication.getName();

        return userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Authenticated user not found"));
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

    private BigDecimal scaleQty(BigDecimal value) {
        return value.setScale(QTY_SCALE, RoundingMode.HALF_UP);
    }
}
