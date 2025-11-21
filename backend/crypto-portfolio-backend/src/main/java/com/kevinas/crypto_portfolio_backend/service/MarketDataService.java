package com.kevinas.crypto_portfolio_backend.service;

import java.math.BigDecimal;
import java.util.Map;

public interface MarketDataService {
    Map<String, BigDecimal> getCurrentPricesForSymbols(Iterable<String> symbols);
    void refreshPriceCache();
}