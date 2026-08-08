package com.kevinas.crypto_portfolio_backend.dto;

import java.math.BigDecimal;
import java.time.Instant;

public record PortfolioPerformancePointResponse(
        Instant snapshotAt,
        BigDecimal totalInvestedUsd,
        BigDecimal totalCurrentValueUsd,
        BigDecimal totalProfitLossUsd
) {
}
