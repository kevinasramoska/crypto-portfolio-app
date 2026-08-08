package com.kevinas.crypto_portfolio_backend.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kevinas.crypto_portfolio_backend.service.MarketDataService;
import org.mockito.Mockito;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;

@TestConfiguration
public class TestConfig {

    @Bean
    public MarketDataService marketDataService() {
        return Mockito.mock(MarketDataService.class);
    }

    @Bean
    public ObjectMapper objectMapper() {
        return new ObjectMapper();
    }
}

