package com.kevinas.crypto_portfolio_backend.service;

import com.kevinas.crypto_portfolio_backend.dto.SupportedCoinResponse;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public interface MarketDataService {
    default com.kevinas.crypto_portfolio_backend.dto.MarketQuote getPriceMetadata(String symbol) {
        return null;
    }
    BigDecimal getCurrentPrice(String symbol);
    Map<String, BigDecimal> getCurrentPrices(List<String> symbols);
    List<SupportedCoinResponse> getSupportedCoins();
}
