package com.kevinas.crypto_portfolio_backend.dto;

import com.kevinas.crypto_portfolio_backend.model.TransactionType;

import java.math.BigDecimal;
import java.time.Instant;

public record TransactionResponse(
        Long id,
        String symbol,
        String name,
        TransactionType type,
        BigDecimal quantity,
        BigDecimal priceUsd,
        BigDecimal totalValueUsd,
        BigDecimal realisedProfitUsd,
        Instant createdAt,
        Instant executedAt
) {
    public TransactionResponse(Long id, String symbol, String name, TransactionType type, BigDecimal quantity,
                               BigDecimal priceUsd, BigDecimal totalValueUsd, BigDecimal realisedProfitUsd, Instant createdAt) {
        this(id, symbol, name, type, quantity, priceUsd, totalValueUsd, realisedProfitUsd, createdAt, createdAt);
    }
}
