package com.kevinas.crypto_portfolio_backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

public record HoldingRequest(
        @NotBlank String symbol,
        @Positive BigDecimal quantity
) {}
