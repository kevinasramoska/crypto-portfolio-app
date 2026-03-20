package com.kevinas.crypto_portfolio_backend.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Crypto Portfolio Tracker API")
                        .version("1.0")
                        .description("Spring Boot backend for managing crypto holdings, market prices, and portfolio analytics"));
    }
}