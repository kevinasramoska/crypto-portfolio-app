package com.kevinas.crypto_portfolio_backend.dto;

import java.math.BigDecimal;

public record PortfolioSummaryResponse(
        BigDecimal totalInvestedUsd,
        BigDecimal totalCurrentValueUsd,
        BigDecimal totalProfitLossUsd
) {}