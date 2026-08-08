package com.kevinas.crypto_portfolio_backend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class CryptoPortfolioBackendApplication {

    public static void main(String[] args) {
        SpringApplication.run(CryptoPortfolioBackendApplication.class, args);
    }
}