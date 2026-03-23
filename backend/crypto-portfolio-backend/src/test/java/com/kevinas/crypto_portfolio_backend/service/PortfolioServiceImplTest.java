package com.kevinas.crypto_portfolio_backend.service;

import com.kevinas.crypto_portfolio_backend.dto.PortfolioSummaryResponse;
import com.kevinas.crypto_portfolio_backend.model.Coin;
import com.kevinas.crypto_portfolio_backend.model.Holding;
import com.kevinas.crypto_portfolio_backend.model.User;
import com.kevinas.crypto_portfolio_backend.repository.HoldingRepository;
import com.kevinas.crypto_portfolio_backend.repository.TransactionRepository;
import com.kevinas.crypto_portfolio_backend.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PortfolioServiceImplTest {

    @Mock
    private HoldingRepository holdingRepository;

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private MarketDataService marketDataService;

    @InjectMocks
    private PortfolioServiceImpl portfolioService;

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void getPortfolioSummary_shouldCalculateTotalsCorrectly_forSingleBtcHolding() {
        String email = "test@example.com";
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(email, null)
        );

        User user = new User();
        user.setId(1L);
        user.setEmail(email);
        user.setPassword("encoded-password");
        user.setCreatedAt(Instant.now());

        Coin btc = new Coin();
        btc.setId(1L);
        btc.setSymbol("BTC");
        btc.setName("Bitcoin");

        Holding holding = new Holding();
        holding.setId(1L);
        holding.setUser(user);
        holding.setCoin(btc);
        holding.setQuantity(new BigDecimal("0.50000000"));
        holding.setAverageBuyPriceUsd(new BigDecimal("45000.00"));
        holding.setCreatedAt(Instant.now());
        holding.setUpdatedAt(Instant.now());

        when(userRepository.findByEmail(email)).thenReturn(Optional.of(user));
        when(holdingRepository.findByUser(user)).thenReturn(List.of(holding));
        when(transactionRepository.findByUserOrderByCreatedAtDesc(user)).thenReturn(List.of());
        when(marketDataService.getCurrentPrice("BTC")).thenReturn(new BigDecimal("70834.00"));

        PortfolioSummaryResponse response = portfolioService.getPortfolioSummary();

        assertEquals(new BigDecimal("22500.00"), response.getTotalInvestedUsd());
        assertEquals(new BigDecimal("35417.00"), response.getTotalCurrentValueUsd());
        assertEquals(new BigDecimal("12917.00"), response.getTotalUnrealisedProfitLossUsd());
        assertEquals(new BigDecimal("0.00"), response.getTotalRealisedProfitLossUsd());
        assertEquals(new BigDecimal("12917.00"), response.getTotalProfitLossUsd());

        assertEquals(1, response.getHoldings().size());

        var holdingSummary = response.getHoldings().get(0);
        assertEquals("BTC", holdingSummary.getSymbol());
        assertEquals("Bitcoin", holdingSummary.getName());
        assertEquals(new BigDecimal("0.50000000"), holdingSummary.getQuantity());
        assertEquals(new BigDecimal("45000.00"), holdingSummary.getAverageBuyPriceUsd());
        assertEquals(new BigDecimal("70834.00"), holdingSummary.getCurrentPriceUsd());
        assertEquals(new BigDecimal("22500.00"), holdingSummary.getInvestedValueUsd());
        assertEquals(new BigDecimal("35417.00"), holdingSummary.getCurrentValueUsd());
        assertEquals(new BigDecimal("12917.00"), holdingSummary.getUnrealisedProfitLossUsd());
    }
}