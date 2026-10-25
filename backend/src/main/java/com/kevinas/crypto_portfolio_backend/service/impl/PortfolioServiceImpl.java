package com.kevinas.crypto_portfolio_backend.service.impl;

import com.kevinas.crypto_portfolio_backend.dto.HoldingResponse;
import com.kevinas.crypto_portfolio_backend.dto.PortfolioHoldingSummaryResponse;
import com.kevinas.crypto_portfolio_backend.dto.PortfolioPerformanceHistoryResponse;
import com.kevinas.crypto_portfolio_backend.dto.PortfolioPerformancePointResponse;
import com.kevinas.crypto_portfolio_backend.dto.PortfolioSummaryResponse;
import com.kevinas.crypto_portfolio_backend.model.Holding;
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
    private final PortfolioSnapshotWriter snapshotWriter;

    @Override
    public List<HoldingResponse> getUserHoldings() {
        User user = getAuthenticatedUser();

        List<Holding> holdings = holdingRepository.findByUser(user);
        marketDataService.getCurrentPrices(holdings.stream().map(h -> h.getCoin().getSymbol()).toList());
        return holdings.stream()
                .map(holding -> {
                    PriceLookup currentPrice = lookupPrice(holding.getCoin().getSymbol());
                    BigDecimal exactCost = costBasis(holding);
                    BigDecimal exactValue = holding.getQuantity().multiply(currentPrice.price());
                    BigDecimal investedValueUsd = money(exactCost);
                    BigDecimal currentValueUsd = currentPrice.available()
                            ? money(holding.getQuantity().multiply(currentPrice.price()))
                            : BigDecimal.ZERO.setScale(USD_SCALE, RoundingMode.HALF_UP);
                    BigDecimal profitLossUsd = currentPrice.available()
                            ? money(exactValue.subtract(exactCost))
                            : BigDecimal.ZERO.setScale(USD_SCALE, RoundingMode.HALF_UP);

                    return new HoldingResponse(
                            holding.getId(),
                            holding.getCoin().getSymbol(),
                            assetName(holding),
                            scaleQty(holding.getQuantity()),
                            holding.getAverageBuyPriceUsd(),
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
    @org.springframework.transaction.annotation.Transactional(propagation = org.springframework.transaction.annotation.Propagation.NOT_SUPPORTED)
    public void createSnapshotForUser(Long userId) {
        captureSnapshot(userId, false);
    }

    @Override
    @org.springframework.transaction.annotation.Transactional(propagation = org.springframework.transaction.annotation.Propagation.NOT_SUPPORTED)
    public void createDailySnapshotForUser(Long userId) {
        captureSnapshot(userId, true);
    }

    private void captureSnapshot(Long userId, boolean daily) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        // Detached post-commit work must fetch quotes before entering the writer transaction.
        List<String> symbols = holdingRepository.findByUser(user).stream().map(h -> h.getCoin().getSymbol()).toList();
        marketDataService.getCurrentPrices(symbols);
        java.util.Map<String, PriceLookup> quotes = new java.util.HashMap<>();
        java.util.Map<String, Boolean> stale = new java.util.HashMap<>();
        for (String symbol : symbols) {
            quotes.put(symbol, lookupPrice(symbol));
            var metadata = marketDataService.getPriceMetadata(symbol);
            stale.put(symbol, metadata != null && !"FRESH".equals(metadata.status()));
        }
        snapshotWriter.write(userId, daily, locked -> computePortfolioSummary(locked, quotes, stale));
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
                        money(snapshot.getTotalProfitLossUsd()),
                        snapshot.isValuationComplete()
                ))
                .toList();

        return new PortfolioPerformanceHistoryResponse(range.toLowerCase(), history);
    }

    private PortfolioSummaryResponse computePortfolioSummary(User user) {
        return computePortfolioSummary(user, null, null);
    }

    private PortfolioSummaryResponse computePortfolioSummary(User user, java.util.Map<String, PriceLookup> quotes,
            java.util.Map<String, Boolean> stale) {
        List<Holding> holdings = holdingRepository.findByUser(user);
        if (quotes == null) marketDataService.getCurrentPrices(holdings.stream().map(h -> h.getCoin().getSymbol()).toList());

        List<PortfolioHoldingSummaryResponse> holdingSummaries = new ArrayList<>();
        BigDecimal totalInvestedUsd = BigDecimal.ZERO;
        BigDecimal totalCurrentValueUsd = BigDecimal.ZERO;
        BigDecimal totalUnrealisedProfitLossUsd = BigDecimal.ZERO;
        boolean hasUnsupportedMarketData = false;
        boolean hasStaleMarketData = false;

        for (Holding holding : holdings) {
            if (holding.getQuantity().compareTo(BigDecimal.ZERO) <= 0) {
                continue;
            }

            String symbol = holding.getCoin().getSymbol();
            PriceLookup currentPrice = quotes == null ? lookupPrice(symbol) : quotes.getOrDefault(symbol, PriceLookup.unavailable());
            BigDecimal exactCost = costBasis(holding);
            BigDecimal exactValue = currentPrice.available() ? holding.getQuantity().multiply(currentPrice.price()) : BigDecimal.ZERO;
            BigDecimal exactUnrealised = currentPrice.available() ? exactValue.subtract(exactCost) : BigDecimal.ZERO;
            BigDecimal investedValueUsd = money(exactCost);
            BigDecimal currentValueUsd = currentPrice.available()
                    ? money(holding.getQuantity().multiply(currentPrice.price()))
                    : BigDecimal.ZERO.setScale(USD_SCALE, RoundingMode.HALF_UP);
            BigDecimal unrealisedProfitLossUsd = currentPrice.available()
                    ? money(exactUnrealised)
                    : BigDecimal.ZERO.setScale(USD_SCALE, RoundingMode.HALF_UP);
            hasUnsupportedMarketData = hasUnsupportedMarketData || !currentPrice.available();
            if (stale == null) {
                var metadata = marketDataService.getPriceMetadata(symbol);
                hasStaleMarketData |= metadata != null && !"FRESH".equals(metadata.status());
            } else {
                hasStaleMarketData |= stale.getOrDefault(symbol, true);
            }

            PortfolioHoldingSummaryResponse summary = PortfolioHoldingSummaryResponse.builder()
                    .symbol(holding.getCoin().getSymbol())
                    .name(assetName(holding))
                    .quantity(scaleQty(holding.getQuantity()))
                    .averageBuyPriceUsd(holding.getAverageBuyPriceUsd())
                    .currentPriceUsd(currentPrice.price())
                    .investedValueUsd(investedValueUsd)
                    .currentValueUsd(currentValueUsd)
                    .unrealisedProfitLossUsd(unrealisedProfitLossUsd)
                    .marketPriceAvailable(currentPrice.available())
                    .build();

            holdingSummaries.add(summary);

            totalInvestedUsd = totalInvestedUsd.add(exactCost);
            if (currentPrice.available()) {
                totalCurrentValueUsd = totalCurrentValueUsd.add(exactValue);
                totalUnrealisedProfitLossUsd = totalUnrealisedProfitLossUsd.add(exactUnrealised);
            }
        }

        BigDecimal totalRealisedProfitLossUsd = transactionRepository.aggregateActiveTotals(user).totalRealisedProfitUsd();

        BigDecimal totalProfitLossUsd = money(totalRealisedProfitLossUsd.add(totalUnrealisedProfitLossUsd));
        totalInvestedUsd = money(totalInvestedUsd);
        totalCurrentValueUsd = money(totalCurrentValueUsd);
        totalUnrealisedProfitLossUsd = money(totalUnrealisedProfitLossUsd);
        totalRealisedProfitLossUsd = money(totalRealisedProfitLossUsd);

        return PortfolioSummaryResponse.builder()
                .totalInvestedUsd(totalInvestedUsd)
                .totalCurrentValueUsd(totalCurrentValueUsd)
                .totalUnrealisedProfitLossUsd(totalUnrealisedProfitLossUsd)
                .totalRealisedProfitLossUsd(totalRealisedProfitLossUsd)
                .totalProfitLossUsd(totalProfitLossUsd)
                .holdings(holdingSummaries)
                .hasUnsupportedMarketData(hasUnsupportedMarketData)
                .hasStaleMarketData(hasStaleMarketData)
                .build();
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
            return new PriceLookup(price, true);
        } catch (RuntimeException ex) {
            return PriceLookup.unavailable();
        }
    }

    private String assetName(Holding holding) {
        return holding.getAssetName() == null ? holding.getCoin().getName() : holding.getAssetName();
    }

    private BigDecimal costBasis(Holding holding) {
        return holding.getCostBasisUsd() == null
                ? holding.getQuantity().multiply(holding.getAverageBuyPriceUsd()) : holding.getCostBasisUsd();
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
