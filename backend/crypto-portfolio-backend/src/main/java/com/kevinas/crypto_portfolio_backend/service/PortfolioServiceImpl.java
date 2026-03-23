package com.kevinas.crypto_portfolio_backend.service;

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
import java.util.List;

@Service
@RequiredArgsConstructor
public class PortfolioServiceImpl implements PortfolioService {

    private static final int USD_SCALE = 2;
    private static final int QTY_SCALE = 8;

    private final HoldingRepository holdingRepository;
    private final TransactionRepository transactionRepository;
    private final UserRepository userRepository;
    private final MarketDataService marketDataService;

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
    public PortfolioSummaryResponse getPortfolioSummary() {
        User user = getAuthenticatedUser();

        List<Holding> holdings = holdingRepository.findByUser(user);
        List<Transaction> transactions = transactionRepository.findByUserOrderByCreatedAtDesc(user);

        BigDecimal totalInvestedUsd = BigDecimal.ZERO;
        BigDecimal totalCurrentValueUsd = BigDecimal.ZERO;
        BigDecimal totalUnrealisedProfitLossUsd = BigDecimal.ZERO;

        List<PortfolioHoldingSummaryResponse> holdingSummaries = holdings.stream()
                .map(holding -> {
                    BigDecimal quantity = scaleQty(holding.getQuantity());
                    BigDecimal averageBuyPriceUsd = money(holding.getAverageBuyPriceUsd());
                    BigDecimal currentPriceUsd = safePriceLookup(holding.getCoin().getSymbol());

                    BigDecimal investedValueUsd = money(quantity.multiply(averageBuyPriceUsd));
                    BigDecimal currentValueUsd = money(quantity.multiply(currentPriceUsd));
                    BigDecimal unrealisedProfitLossUsd = money(currentValueUsd.subtract(investedValueUsd));

                    return PortfolioHoldingSummaryResponse.builder()
                            .symbol(holding.getCoin().getSymbol())
                            .name(holding.getCoin().getName())
                            .quantity(quantity)
                            .averageBuyPriceUsd(averageBuyPriceUsd)
                            .currentPriceUsd(currentPriceUsd)
                            .investedValueUsd(investedValueUsd)
                            .currentValueUsd(currentValueUsd)
                            .unrealisedProfitLossUsd(unrealisedProfitLossUsd)
                            .build();
                })
                .toList();

        for (PortfolioHoldingSummaryResponse holding : holdingSummaries) {
            totalInvestedUsd = totalInvestedUsd.add(holding.getInvestedValueUsd());
            totalCurrentValueUsd = totalCurrentValueUsd.add(holding.getCurrentValueUsd());
            totalUnrealisedProfitLossUsd = totalUnrealisedProfitLossUsd.add(holding.getUnrealisedProfitLossUsd());
        }

        BigDecimal totalRealisedProfitLossUsd = transactions.stream()
                .filter(transaction -> transaction.getType() == TransactionType.SELL)
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