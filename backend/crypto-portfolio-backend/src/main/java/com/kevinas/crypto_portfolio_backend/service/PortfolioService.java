package com.kevinas.crypto_portfolio_backend.service;

import com.kevinas.crypto_portfolio_backend.dto.HoldingResponse;
import com.kevinas.crypto_portfolio_backend.dto.PortfolioSummaryResponse;

import java.util.List;

public interface PortfolioService {
    List<HoldingResponse> getUserHoldings();
    PortfolioSummaryResponse getCurrentUserPortfolioSummary();
}