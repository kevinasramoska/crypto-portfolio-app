package com.kevinas.crypto_portfolio_backend.service;

import com.kevinas.crypto_portfolio_backend.dto.HoldingResponse;
import com.kevinas.crypto_portfolio_backend.dto.PortfolioSummaryResponse;
import com.kevinas.crypto_portfolio_backend.model.User;

import java.util.List;

public interface PortfolioService {
    List<HoldingResponse> getUserHoldings();
    PortfolioSummaryResponse getCurrentUserPortfolioSummary();
    PortfolioSummaryResponse getPortfolioSummaryForUser(User user);
}