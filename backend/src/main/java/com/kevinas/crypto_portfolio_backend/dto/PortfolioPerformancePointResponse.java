package com.kevinas.crypto_portfolio_backend.dto;

import java.math.BigDecimal;
import java.time.Instant;

public record PortfolioPerformancePointResponse(
        Instant snapshotAt,
        BigDecimal totalInvestedUsd,
        BigDecimal totalCurrentValueUsd,
        BigDecimal totalProfitLossUsd,
        boolean valuationComplete
) {
    public PortfolioPerformancePointResponse(Instant at, BigDecimal invested, BigDecimal current, BigDecimal profit) {
        this(at, invested, current, profit, false);
    }
}
