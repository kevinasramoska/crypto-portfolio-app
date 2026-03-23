package com.kevinas.crypto_portfolio_backend.service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public interface MarketDataService {
    BigDecimal getCurrentPrice(String symbol);
    Map<String, BigDecimal> getCurrentPrices(List<String> symbols);
}