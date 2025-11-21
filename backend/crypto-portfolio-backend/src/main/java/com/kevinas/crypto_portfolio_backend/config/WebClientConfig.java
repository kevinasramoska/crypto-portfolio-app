package com.kevinas.crypto_portfolio_backend.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class WebClientConfig {

    @Bean
    public WebClient coinGeckoClient() {
        return WebClient.builder()
                .baseUrl("https://api.coingecko.com/api/v3")
                .build();
    }
}
