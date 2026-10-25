package com.kevinas.crypto_portfolio_backend.dto;

import java.math.BigDecimal;
import java.time.Instant;

public record MarketQuote(BigDecimal price, Instant fetchedAt, String status) {}
