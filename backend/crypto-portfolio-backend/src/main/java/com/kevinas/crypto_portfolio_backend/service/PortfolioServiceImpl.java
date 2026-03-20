package com.kevinas.crypto_portfolio_backend.service;

import com.kevinas.crypto_portfolio_backend.dto.HoldingRequest;
import com.kevinas.crypto_portfolio_backend.dto.HoldingResponse;
import com.kevinas.crypto_portfolio_backend.dto.PortfolioSummaryResponse;
import com.kevinas.crypto_portfolio_backend.model.Coin;
import com.kevinas.crypto_portfolio_backend.model.Holding;
import com.kevinas.crypto_portfolio_backend.model.User;
import com.kevinas.crypto_portfolio_backend.repository.CoinRepository;
import com.kevinas.crypto_portfolio_backend.repository.HoldingRepository;
import com.kevinas.crypto_portfolio_backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class PortfolioServiceImpl implements PortfolioService {

    private final HoldingRepository holdingRepository;
    private final CoinRepository coinRepository;
    private final UserRepository userRepository;
    private final MarketDataService marketDataService;

    @Override
    public List<HoldingResponse> getUserHoldings() {
        User user = getCurrentUser();
        List<Holding> holdings = holdingRepository.findByUser(user);

        List<String> symbols = holdings.stream()
                .map(h -> h.getCoin().getSymbol())
                .toList();

        Map<String, BigDecimal> prices = marketDataService.getCurrentPricesForSymbols(symbols);

        return holdings.stream()
                .map(h -> toResponse(h, prices))
                .toList();
    }

    @Override
    public HoldingResponse addHolding(HoldingRequest request) {
        User user = getCurrentUser();

        Coin coin = coinRepository.findBySymbolIgnoreCase(request.symbol())
                .orElseGet(() -> coinRepository.save(
                        Coin.builder()
                                .symbol(request.symbol().toUpperCase())
                                .name(request.name())
                                .build()
                ));

        Holding holding = Holding.builder()
                .user(user)
                .coin(coin)
                .quantity(request.quantity())
                .averageBuyPriceUsd(request.averageBuyPriceUsd())
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        Holding saved = holdingRepository.save(holding);
        Map<String, BigDecimal> prices = marketDataService.getCurrentPricesForSymbols(List.of(saved.getCoin().getSymbol()));
        return toResponse(saved, prices);
    }

    @Override
    public HoldingResponse updateHolding(Long id, HoldingRequest request) {
        User user = getCurrentUser();

        Holding holding = holdingRepository.findById(id)
                .filter(h -> h.getUser().getId().equals(user.getId()))
                .orElseThrow(() -> new IllegalArgumentException("Holding not found"));

        Coin coin = coinRepository.findBySymbolIgnoreCase(request.symbol())
                .orElseGet(() -> coinRepository.save(
                        Coin.builder()
                                .symbol(request.symbol().toUpperCase())
                                .name(request.name())
                                .build()
                ));

        holding.setCoin(coin);
        holding.setQuantity(request.quantity());
        holding.setAverageBuyPriceUsd(request.averageBuyPriceUsd());
        holding.setUpdatedAt(Instant.now());

        Holding saved = holdingRepository.save(holding);
        Map<String, BigDecimal> prices = marketDataService.getCurrentPricesForSymbols(List.of(saved.getCoin().getSymbol()));
        return toResponse(saved, prices);
    }

    @Override
    public void deleteHolding(Long id) {
        User user = getCurrentUser();

        Holding holding = holdingRepository.findById(id)
                .filter(h -> h.getUser().getId().equals(user.getId()))
                .orElseThrow(() -> new IllegalArgumentException("Holding not found"));

        holdingRepository.delete(holding);
    }

    @Override
    public PortfolioSummaryResponse getPortfolioSummary() {
        List<HoldingResponse> holdings = getUserHoldings();

        BigDecimal totalInvested = holdings.stream()
                .map(HoldingResponse::investedValueUsd)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalCurrentValue = holdings.stream()
                .map(HoldingResponse::currentValueUsd)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalProfitLoss = totalCurrentValue.subtract(totalInvested);

        return new PortfolioSummaryResponse(
                scale(totalInvested),
                scale(totalCurrentValue),
                scale(totalProfitLoss)
        );
    }

    private HoldingResponse toResponse(Holding holding, Map<String, BigDecimal> prices) {
        BigDecimal currentPrice = prices.getOrDefault(
                holding.getCoin().getSymbol().toUpperCase(),
                BigDecimal.ZERO
        );

        BigDecimal investedValue = holding.getQuantity().multiply(holding.getAverageBuyPriceUsd());
        BigDecimal currentValue = holding.getQuantity().multiply(currentPrice);
        BigDecimal profitLoss = currentValue.subtract(investedValue);

        return new HoldingResponse(
                holding.getId(),
                holding.getCoin().getSymbol(),
                holding.getCoin().getName(),
                scale(holding.getQuantity()),
                scale(holding.getAverageBuyPriceUsd()),
                scale(currentPrice),
                scale(investedValue),
                scale(currentValue),
                scale(profitLoss)
        );
    }

    private User getCurrentUser() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalStateException("Logged in user not found"));
    }

    private BigDecimal scale(BigDecimal value) {
        return value.setScale(2, RoundingMode.HALF_UP);
    }
}