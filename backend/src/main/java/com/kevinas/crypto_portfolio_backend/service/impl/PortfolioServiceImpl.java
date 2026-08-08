package com.kevinas.crypto_portfolio_backend.service.impl;

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
import com.kevinas.crypto_portfolio_backend.service.MarketDataService;
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
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PortfolioServiceImpl implements com.kevinas.crypto_portfolio_backend.service.PortfolioService {

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
                    PriceLookup currentPrice = lookupPrice(holding.getCoin().getSymbol());
                    BigDecimal investedValueUsd = money(
                            holding.getQuantity().multiply(holding.getAverageBuyPriceUsd())
                    );
                    BigDecimal currentValueUsd = currentPrice.available()
                            ? money(holding.getQuantity().multiply(currentPrice.price()))
                            : BigDecimal.ZERO.setScale(USD_SCALE, RoundingMode.HALF_UP);
                    BigDecimal profitLossUsd = currentPrice.available()
                            ? money(currentValueUsd.subtract(investedValueUsd))
                            : BigDecimal.ZERO.setScale(USD_SCALE, RoundingMode.HALF_UP);

                    return new HoldingResponse(
                            holding.getId(),
                            holding.getCoin().getSymbol(),
                            holding.getCoin().getName(),
                            scaleQty(holding.getQuantity()),
                            money(holding.getAverageBuyPriceUsd()),
                            currentPrice.price(),
                            investedValueUsd,
                            currentValueUsd,
                            profitLossUsd,
                            currentPrice.available()
                    );
                })
                .collect(Collectors.toList());
    }

    @Override
    public PortfolioSummaryResponse getCurrentUserPortfolioSummary() {
        User user = getAuthenticatedUser();
        return computePortfolioSummary(user);
    }

    @Override
    public PortfolioSummaryResponse getPortfolioSummaryForUser(User user) {
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
        List<Holding> holdings = holdingRepository.findByUser(user);
        List<Transaction> transactions = transactionRepository.findByUserOrderByCreatedAtAsc(user);

        List<PortfolioHoldingSummaryResponse> holdingSummaries = new ArrayList<>();
        BigDecimal totalInvestedUsd = BigDecimal.ZERO;
        BigDecimal totalCurrentValueUsd = BigDecimal.ZERO;
        BigDecimal totalUnrealisedProfitLossUsd = BigDecimal.ZERO;
        boolean hasUnsupportedMarketData = false;

        for (Holding holding : holdings) {
            if (holding.getQuantity().compareTo(BigDecimal.ZERO) <= 0) {
                continue;
            }

            PriceLookup currentPrice = lookupPrice(holding.getCoin().getSymbol());
            BigDecimal investedValueUsd = money(
                    holding.getQuantity().multiply(holding.getAverageBuyPriceUsd())
            );
            BigDecimal currentValueUsd = currentPrice.available()
                    ? money(holding.getQuantity().multiply(currentPrice.price()))
                    : BigDecimal.ZERO.setScale(USD_SCALE, RoundingMode.HALF_UP);
            BigDecimal unrealisedProfitLossUsd = currentPrice.available()
                    ? money(currentValueUsd.subtract(investedValueUsd))
                    : BigDecimal.ZERO.setScale(USD_SCALE, RoundingMode.HALF_UP);
            hasUnsupportedMarketData = hasUnsupportedMarketData || !currentPrice.available();

            PortfolioHoldingSummaryResponse summary = PortfolioHoldingSummaryResponse.builder()
                    .symbol(holding.getCoin().getSymbol())
                    .name(holding.getCoin().getName())
                    .quantity(scaleQty(holding.getQuantity()))
                    .averageBuyPriceUsd(money(holding.getAverageBuyPriceUsd()))
                    .currentPriceUsd(currentPrice.price())
                    .investedValueUsd(investedValueUsd)
                    .currentValueUsd(currentValueUsd)
                    .unrealisedProfitLossUsd(unrealisedProfitLossUsd)
                    .marketPriceAvailable(currentPrice.available())
                    .build();

            holdingSummaries.add(summary);

            totalInvestedUsd = totalInvestedUsd.add(investedValueUsd);
            if (currentPrice.available()) {
                totalCurrentValueUsd = totalCurrentValueUsd.add(currentValueUsd);
                totalUnrealisedProfitLossUsd = totalUnrealisedProfitLossUsd.add(unrealisedProfitLossUsd);
            }
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
                .hasUnsupportedMarketData(hasUnsupportedMarketData)
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
        String username = authentication.getName();
        return userRepository.findByEmail(username)
                .orElseThrow(() -> new RuntimeException("User not found: " + username));
    }

    private PriceLookup lookupPrice(String symbol) {
        try {
            BigDecimal price = marketDataService.getCurrentPrice(symbol);
            if (price == null || price.compareTo(BigDecimal.ZERO) <= 0) {
                return PriceLookup.unavailable();
            }
            return new PriceLookup(money(price), true);
        } catch (RuntimeException ex) {
            return PriceLookup.unavailable();
        }
    }

    private BigDecimal money(BigDecimal value) {
        return value.setScale(USD_SCALE, RoundingMode.HALF_UP);
    }

    private BigDecimal scaleQty(BigDecimal quantity) {
        return quantity.setScale(QTY_SCALE, RoundingMode.HALF_UP);
    }

    private record PriceLookup(BigDecimal price, boolean available) {
        private static PriceLookup unavailable() {
            return new PriceLookup(BigDecimal.ZERO.setScale(USD_SCALE, RoundingMode.HALF_UP), false);
        }
    }
}
