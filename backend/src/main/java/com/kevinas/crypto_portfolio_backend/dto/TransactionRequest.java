package com.kevinas.crypto_portfolio_backend.dto;

import com.kevinas.crypto_portfolio_backend.model.TransactionType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record TransactionRequest(
        @NotBlank(message = "Symbol is required")
        @Pattern(regexp = "[A-Z0-9]{1,10}", message = "Symbol must contain 1–10 letters or digits")
        String symbol,

        @NotBlank(message = "Name is required")
        @Size(max = 255)
        String name,

        @NotNull(message = "Type is required")
        TransactionType type,

        @NotNull(message = "Quantity is required")
        @Positive(message = "Quantity must be greater than 0")
        @Digits(integer = 11, fraction = 8)
        BigDecimal quantity,

        @NotNull(message = "Price is required")
        @DecimalMin(value = "0.00", message = "Price must be 0 or greater")
        @Digits(integer = 12, fraction = 12)
        BigDecimal priceUsd,

        @jakarta.validation.constraints.PastOrPresent(message = "Execution time cannot be in the future")
        java.time.Instant executedAt
) {
    public TransactionRequest(String symbol, String name, TransactionType type, BigDecimal quantity, BigDecimal priceUsd) {
        this(symbol, name, type, quantity, priceUsd, null);
    }

    public TransactionRequest {
        symbol = symbol == null ? null : symbol.trim().toUpperCase(java.util.Locale.ROOT);
        name = name == null ? null : name.trim();
    }
}
