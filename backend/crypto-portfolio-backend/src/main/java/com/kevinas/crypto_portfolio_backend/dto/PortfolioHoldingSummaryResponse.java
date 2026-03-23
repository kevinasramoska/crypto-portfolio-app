package com.kevinas.crypto_portfolio_backend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PortfolioHoldingSummaryResponse {
    private String symbol;
    private String name;
    private BigDecimal quantity;
    private BigDecimal averageBuyPriceUsd;
    private BigDecimal currentPriceUsd;
    private BigDecimal investedValueUsd;
    private BigDecimal currentValueUsd;
    private BigDecimal unrealisedProfitLossUsd;
}