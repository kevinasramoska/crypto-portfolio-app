package com.kevinas.crypto_portfolio_backend.service;

import com.kevinas.crypto_portfolio_backend.dto.PortfolioSummaryResponse;
import com.kevinas.crypto_portfolio_backend.model.*;
import com.kevinas.crypto_portfolio_backend.repository.TransactionRepository;
import com.kevinas.crypto_portfolio_backend.repository.UserRepository;
import com.kevinas.crypto_portfolio_backend.service.impl.PortfolioServiceImpl;
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

        Transaction buyTransaction = new Transaction();
        buyTransaction.setId(1L);
        buyTransaction.setUser(user);
        buyTransaction.setCoin(btc);
        buyTransaction.setType(TransactionType.BUY);
        buyTransaction.setQuantity(new BigDecimal("0.50000000"));
        buyTransaction.setPriceUsd(new BigDecimal("45000.00"));
        buyTransaction.setTotalValueUsd(new BigDecimal("22500.00"));
        buyTransaction.setRealisedProfitUsd(new BigDecimal("0.00"));
        buyTransaction.setCreatedAt(Instant.now());

        when(userRepository.findByEmail(email)).thenReturn(Optional.of(user));
        when(transactionRepository.findByUserOrderByCreatedAtAsc(user)).thenReturn(List.of(buyTransaction));
        when(marketDataService.getCurrentPrice("BTC")).thenReturn(new BigDecimal("70834.00"));

        PortfolioSummaryResponse response = portfolioService.getCurrentUserPortfolioSummary();

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

    @Test
    void getPortfolioSummaryForUser_shouldCalculateTotalsCorrectly_forSingleBtcHolding() {
        User user = new User();
        user.setId(1L);
        user.setEmail("test@example.com");
        user.setPassword("encoded-password");
        user.setCreatedAt(Instant.now());

        Coin btc = new Coin();
        btc.setId(1L);
        btc.setSymbol("BTC");
        btc.setName("Bitcoin");

        Transaction buyTransaction = new Transaction();
        buyTransaction.setId(1L);
        buyTransaction.setUser(user);
        buyTransaction.setCoin(btc);
        buyTransaction.setType(TransactionType.BUY);
        buyTransaction.setQuantity(new BigDecimal("0.50000000"));
        buyTransaction.setPriceUsd(new BigDecimal("45000.00"));
        buyTransaction.setTotalValueUsd(new BigDecimal("22500.00"));
        buyTransaction.setRealisedProfitUsd(new BigDecimal("0.00"));
        buyTransaction.setCreatedAt(Instant.now());

        when(transactionRepository.findByUserOrderByCreatedAtAsc(user)).thenReturn(List.of(buyTransaction));
        when(marketDataService.getCurrentPrice("BTC")).thenReturn(new BigDecimal("70834.00"));

        PortfolioSummaryResponse response = portfolioService.getPortfolioSummaryForUser(user);

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

    @Test
    void getPortfolioSummary_shouldCalculateRealisedAndUnrealisedProfitCorrectly_afterMultipleBuysAndSell() {
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

        Transaction buyTransaction = new Transaction();
        buyTransaction.setId(1L);
        buyTransaction.setUser(user);
        buyTransaction.setCoin(btc);
        buyTransaction.setType(TransactionType.BUY);
        buyTransaction.setQuantity(new BigDecimal("1.00000000"));
        buyTransaction.setPriceUsd(new BigDecimal("50000.00"));
        buyTransaction.setTotalValueUsd(new BigDecimal("50000.00"));
        buyTransaction.setRealisedProfitUsd(new BigDecimal("0.00"));
        buyTransaction.setCreatedAt(Instant.now().minusSeconds(10)); // Earlier

        Transaction sellTransaction = new Transaction();
        sellTransaction.setId(2L);
        sellTransaction.setUser(user);
        sellTransaction.setCoin(btc);
        sellTransaction.setType(TransactionType.SELL);
        sellTransaction.setQuantity(new BigDecimal("0.20000000"));
        sellTransaction.setPriceUsd(new BigDecimal("60000.00"));
        sellTransaction.setTotalValueUsd(new BigDecimal("12000.00"));
        sellTransaction.setRealisedProfitUsd(new BigDecimal("2000.00"));
        sellTransaction.setCreatedAt(Instant.now()); // Later

        when(userRepository.findByEmail(email)).thenReturn(Optional.of(user));
        when(transactionRepository.findByUserOrderByCreatedAtAsc(user)).thenReturn(List.of(buyTransaction, sellTransaction));
        when(marketDataService.getCurrentPrice("BTC")).thenReturn(new BigDecimal("65000.00"));

        PortfolioSummaryResponse response = portfolioService.getCurrentUserPortfolioSummary();

        assertEquals(new BigDecimal("40000.00"), response.getTotalInvestedUsd());
        assertEquals(new BigDecimal("52000.00"), response.getTotalCurrentValueUsd());
        assertEquals(new BigDecimal("12000.00"), response.getTotalUnrealisedProfitLossUsd());
        assertEquals(new BigDecimal("2000.00"), response.getTotalRealisedProfitLossUsd());
        assertEquals(new BigDecimal("14000.00"), response.getTotalProfitLossUsd());

        assertEquals(1, response.getHoldings().size());

        var holdingSummary = response.getHoldings().get(0);
        assertEquals("BTC", holdingSummary.getSymbol());
        assertEquals("Bitcoin", holdingSummary.getName());
        assertEquals(new BigDecimal("0.80000000"), holdingSummary.getQuantity());
        assertEquals(new BigDecimal("50000.00"), holdingSummary.getAverageBuyPriceUsd());
        assertEquals(new BigDecimal("65000.00"), holdingSummary.getCurrentPriceUsd());
        assertEquals(new BigDecimal("40000.00"), holdingSummary.getInvestedValueUsd());
        assertEquals(new BigDecimal("52000.00"), holdingSummary.getCurrentValueUsd());
        assertEquals(new BigDecimal("12000.00"), holdingSummary.getUnrealisedProfitLossUsd());
    }
}