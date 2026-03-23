package com.kevinas.crypto_portfolio_backend.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class MarketDataServiceImpl implements MarketDataService {

    private final RestTemplate restTemplate;

    private static final Map<String, String> SYMBOL_TO_COINGECKO_ID = Map.ofEntries(
            Map.entry("BTC", "bitcoin"),
            Map.entry("ETH", "ethereum"),
            Map.entry("SOL", "solana"),
            Map.entry("ADA", "cardano"),
            Map.entry("XRP", "ripple"),
            Map.entry("DOGE", "dogecoin"),
            Map.entry("DOT", "polkadot"),
            Map.entry("AVAX", "avalanche-2"),
            Map.entry("MATIC", "matic-network"),
            Map.entry("LINK", "chainlink"),
            Map.entry("LTC", "litecoin"),
            Map.entry("BNB", "binancecoin")
    );

    @Override
    public BigDecimal getCurrentPrice(String symbol) {
        if (symbol == null || symbol.isBlank()) {
            return BigDecimal.ZERO;
        }

        Map<String, BigDecimal> prices = getCurrentPrices(List.of(symbol));
        return prices.getOrDefault(symbol.toUpperCase(Locale.ROOT), BigDecimal.ZERO);
    }

    @Override
    public Map<String, BigDecimal> getCurrentPrices(List<String> symbols) {
        Map<String, BigDecimal> result = new HashMap<>();

        if (symbols == null || symbols.isEmpty()) {
            return result;
        }

        Map<String, String> symbolToId = new HashMap<>();
        for (String symbol : symbols) {
            String upper = symbol.toUpperCase(Locale.ROOT);
            String coinId = SYMBOL_TO_COINGECKO_ID.get(upper);
            if (coinId != null) {
                symbolToId.put(upper, coinId);
            }
        }

        if (symbolToId.isEmpty()) {
            return result;
        }

        String ids = String.join(",", symbolToId.values());
        String url = "https://api.coingecko.com/api/v3/simple/price?ids=" + ids + "&vs_currencies=usd";

        try {
            Map<String, Map<String, Object>> response = restTemplate.getForObject(url, Map.class);

            if (response == null) {
                return result;
            }

            for (Map.Entry<String, String> entry : symbolToId.entrySet()) {
                String symbol = entry.getKey();
                String coinId = entry.getValue();

                Map<String, Object> priceData = response.get(coinId);
                if (priceData != null && priceData.get("usd") != null) {
                    Object usdValue = priceData.get("usd");
                    result.put(symbol, new BigDecimal(usdValue.toString()));
                }
            }
        } catch (Exception ignored) {
            // Fail safely for now.
        }

        return result;
    }
}