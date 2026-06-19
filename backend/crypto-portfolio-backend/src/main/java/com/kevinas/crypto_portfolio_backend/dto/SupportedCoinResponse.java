package com.kevinas.crypto_portfolio_backend.dto;

public record SupportedCoinResponse(
        String symbol,
        String name,
        String coinGeckoId
) {
}
