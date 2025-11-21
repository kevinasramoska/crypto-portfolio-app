package com.kevinas.crypto_portfolio_backend.dto;

import java.math.BigDecimal;

public record HoldingResponse(
        Long id,
        String symbol,
        String name,
        BigDecimal quantity
) {}