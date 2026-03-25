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
import static org.mockito.Mockito.*;

class MarketDataServiceImplTest {

    private RestTemplate restTemplate;
    private MarketDataServiceImpl marketDataService;

    @BeforeEach
    void setUp() {
        restTemplate = mock(RestTemplate.class);
        marketDataService = new MarketDataServiceImpl(restTemplate);
    }

    // 1. Original behaviour (still required)
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

    // 2. Cache should prevent second API call
    @Test
    void getCurrentPrices_shouldUseCacheWithinTtl() {
        Map<String, Map<String, Object>> apiResponse = Map.of(
                "bitcoin", Map.of("usd", 60000)
        );

        when(restTemplate.getForObject(anyString(), eq(Map.class)))
                .thenReturn(apiResponse);

        // first call → hits API
        Map<String, BigDecimal> first = marketDataService.getCurrentPrices(List.of("BTC"));

        // second call → should use cache
        Map<String, BigDecimal> second = marketDataService.getCurrentPrices(List.of("BTC"));

        assertEquals(new BigDecimal("60000"), first.get("BTC"));
        assertEquals(new BigDecimal("60000"), second.get("BTC"));

        // verify API called only once
        verify(restTemplate, times(1)).getForObject(anyString(), eq(Map.class));
    }

    // 3. Fallback to cached value if API fails
    @Test
    void getCurrentPrices_shouldFallbackToCachedValue_whenApiFails() {
        Map<String, Map<String, Object>> apiResponse = Map.of(
                "bitcoin", Map.of("usd", 60000)
        );

        when(restTemplate.getForObject(anyString(), eq(Map.class)))
                .thenReturn(apiResponse) // first call OK
                .thenThrow(new RuntimeException("API down")); // second call fails

        // populate cache
        Map<String, BigDecimal> first = marketDataService.getCurrentPrices(List.of("BTC"));

        // simulate another call (will attempt refresh but fail)
        Map<String, BigDecimal> second = marketDataService.getCurrentPrices(List.of("BTC"));

        assertEquals(new BigDecimal("60000"), first.get("BTC"));
        assertEquals(new BigDecimal("60000"), second.get("BTC")); // fallback worked
    }
}