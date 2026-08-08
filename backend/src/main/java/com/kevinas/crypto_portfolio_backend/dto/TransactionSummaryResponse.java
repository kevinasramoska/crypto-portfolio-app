package com.kevinas.crypto_portfolio_backend.dto;

import java.math.BigDecimal;

public record TransactionSummaryResponse(
        BigDecimal totalBuyVolumeUsd,
        BigDecimal totalSellVolumeUsd,
        BigDecimal totalRealisedProfitUsd
) {}