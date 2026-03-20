package com.kevinas.crypto_portfolio_backend.service;

import com.kevinas.crypto_portfolio_backend.dto.HoldingRequest;
import com.kevinas.crypto_portfolio_backend.dto.HoldingResponse;
import com.kevinas.crypto_portfolio_backend.dto.PortfolioSummaryResponse;

import java.util.List;

public interface PortfolioService {
    List<HoldingResponse> getUserHoldings();
    HoldingResponse addHolding(HoldingRequest request);
    HoldingResponse updateHolding(Long id, HoldingRequest request);
    void deleteHolding(Long id);
    PortfolioSummaryResponse getPortfolioSummary();
}