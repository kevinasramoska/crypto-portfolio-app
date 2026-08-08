package com.kevinas.crypto_portfolio_backend.dto;

public record JwtResponse(
        String accessToken,
        String tokenType
) {
    public JwtResponse(String accessToken) {
        this(accessToken, "Bearer");
    }
}