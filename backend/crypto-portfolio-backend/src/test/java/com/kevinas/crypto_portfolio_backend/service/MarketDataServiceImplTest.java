package com.kevinas.crypto_portfolio_backend.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class MarketDataServiceImplTest {

    private RestTemplate restTemplate;
    private MarketDataServiceImpl marketDataService;

    @BeforeEach
    void setUp() {
        restTemplate = mock(RestTemplate.class);
        marketDataService = new MarketDataServiceImpl(restTemplate);
    }

    @Test
    void getCurrentPrices_shouldReturnMappedPrices() {
        Map<String, Map<String, Object>> apiResponse = Map.of(
                "bitcoin", Map.of("usd", 60000),
                "ethereum", Map.of("usd", 2500)
        );

        when(restTemplate.getForObject(anyString(), eq(Map.class)))
                .thenReturn(apiResponse);

        Map<String, BigDecimal> prices = marketDataService.getCurrentPrices(
                List.of("BTC", "ETH")
        );

        assertEquals(new BigDecimal("60000"), prices.get("BTC"));
        assertEquals(new BigDecimal("2500"), prices.get("ETH"));
    }
}