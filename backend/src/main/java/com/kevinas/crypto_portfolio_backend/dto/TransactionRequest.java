package com.kevinas.crypto_portfolio_backend.dto;

import com.kevinas.crypto_portfolio_backend.model.TransactionType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

public record TransactionRequest(
        @NotBlank(message = "Symbol is required")
        String symbol,

        @NotBlank(message = "Name is required")
        String name,

        @NotNull(message = "Type is required")
        TransactionType type,

        @NotNull(message = "Quantity is required")
        @Positive(message = "Quantity must be greater than 0")
        BigDecimal quantity,

        @NotNull(message = "Price is required")
        @DecimalMin(value = "0.00", message = "Price must be 0 or greater")
        BigDecimal priceUsd
) {}