package com.kevinas.crypto_portfolio_backend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PortfolioSummaryResponse {
    private BigDecimal totalInvestedUsd;
    private BigDecimal totalCurrentValueUsd;
    private BigDecimal totalUnrealisedProfitLossUsd;
    private BigDecimal totalRealisedProfitLossUsd;
    private BigDecimal totalProfitLossUsd;
    private List<PortfolioHoldingSummaryResponse> holdings;
}