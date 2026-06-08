package com.kevinas.crypto_portfolio_backend.dto;

import java.util.List;

public record PortfolioPerformanceHistoryResponse(
        String range,
        List<PortfolioPerformancePointResponse> history
) {
}
