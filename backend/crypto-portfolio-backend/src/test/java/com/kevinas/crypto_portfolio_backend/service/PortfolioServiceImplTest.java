package com.kevinas.crypto_portfolio_backend.service;

import com.kevinas.crypto_portfolio_backend.dto.HoldingResponse;
import com.kevinas.crypto_portfolio_backend.dto.PortfolioSummaryResponse;
import com.kevinas.crypto_portfolio_backend.repository.CoinRepository;
import com.kevinas.crypto_portfolio_backend.repository.HoldingRepository;
import com.kevinas.crypto_portfolio_backend.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.doReturn;

@ExtendWith(MockitoExtension.class)
class PortfolioServiceImplTest {

    @Mock
    private HoldingRepository holdingRepository;

    @Mock
    private CoinRepository coinRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private MarketDataService marketDataService;

    @Spy
    @InjectMocks
    private PortfolioServiceImpl portfolioService;

    @Test
    void getPortfolioSummary_shouldCalculateTotalsCorrectly() {
        List<HoldingResponse> holdings = List.of(
                new HoldingResponse(
                        1L, "BTC", "Bitcoin",
                        new BigDecimal("0.50000000"),
                        new BigDecimal("45000.00"),
                        new BigDecimal("60000.00"),
                        new BigDecimal("22500.00"),
                        new BigDecimal("30000.00"),
                        new BigDecimal("7500.00")
                ),
                new HoldingResponse(
                        2L, "ETH", "Ethereum",
                        new BigDecimal("2.00000000"),
                        new BigDecimal("2000.00"),
                        new BigDecimal("2500.00"),
                        new BigDecimal("4000.00"),
                        new BigDecimal("5000.00"),
                        new BigDecimal("1000.00")
                )
        );

        doReturn(holdings).when(portfolioService).getUserHoldings();

        PortfolioSummaryResponse summary = portfolioService.getPortfolioSummary();

        assertEquals(new BigDecimal("26500.00"), summary.totalInvestedUsd());
        assertEquals(new BigDecimal("35000.00"), summary.totalCurrentValueUsd());
        assertEquals(new BigDecimal("8500.00"), summary.totalProfitLossUsd());
    }
}