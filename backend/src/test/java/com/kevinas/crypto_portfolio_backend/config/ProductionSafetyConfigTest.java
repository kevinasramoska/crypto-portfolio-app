package com.kevinas.crypto_portfolio_backend.config;

import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;
import static org.assertj.core.api.Assertions.*;

class ProductionSafetyConfigTest {
    private MockEnvironment valid() {
        return new MockEnvironment().withProperty("jwt.secret", "a-unique-production-key-at-least-thirty-two-bytes")
                .withProperty("spring.datasource.url", "jdbc:postgresql://database.example/app?sslmode=verify-full")
                .withProperty("spring.datasource.username", "app").withProperty("spring.datasource.password", "secret")
                .withProperty("app.cors.allowed-origins", "https://portfolio.example");
    }

    @Test void acceptsExplicitVerifiedConfiguration() {
        assertThatCode(() -> new ProductionSafetyConfig(valid()).validate()).doesNotThrowAnyException();
    }
    @Test void rejectsDevelopmentKeyEvenWhenExplicitlyConfigured() {
        assertThatThrownBy(() -> new ProductionSafetyConfig(valid().withProperty("jwt.secret", "local-development-jwt-secret-32-bytes-minimum")).validate())
                .isInstanceOf(IllegalStateException.class);
    }
    @Test void rejectsUnverifiedTlsAndHttpOrigins() {
        assertThatThrownBy(() -> new ProductionSafetyConfig(valid().withProperty("spring.datasource.url", "jdbc:postgresql://database/app?sslmode=require")).validate())
                .isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> new ProductionSafetyConfig(valid().withProperty("app.cors.allowed-origins", "http://portfolio.example")).validate())
                .isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> new ProductionSafetyConfig(valid().withProperty("spring.datasource.url", "jdbc:postgresql://database/app?sslmode=verify-full&sslmode=disable")).validate())
                .isInstanceOf(IllegalStateException.class);
    }
}
