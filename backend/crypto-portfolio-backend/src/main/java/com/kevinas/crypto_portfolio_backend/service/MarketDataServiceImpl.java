package com.kevinas.crypto_portfolio_backend.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
@Slf4j
public class MarketDataServiceImpl implements MarketDataService {

    private final RestTemplate restTemplate;

    private static final Duration CACHE_TTL = Duration.ofSeconds(60);

    private final Map<String, CachedPrice> priceCache = new ConcurrentHashMap<>();

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

        String normalizedSymbol = symbol.toUpperCase(Locale.ROOT);
        Map<String, BigDecimal> prices = getCurrentPrices(List.of(normalizedSymbol));
        return prices.getOrDefault(normalizedSymbol, BigDecimal.ZERO);
    }

    @Override
    public Map<String, BigDecimal> getCurrentPrices(List<String> symbols) {
        Map<String, BigDecimal> result = new HashMap<>();

        if (symbols == null || symbols.isEmpty()) {
            return result;
        }

        Instant now = Instant.now();
        Map<String, String> symbolToIdToRefresh = new HashMap<>();

        for (String symbol : symbols) {
            if (symbol == null || symbol.isBlank()) {
                continue;
            }

            String upper = symbol.toUpperCase(Locale.ROOT);
            CachedPrice cachedPrice = priceCache.get(upper);

            if (isFresh(cachedPrice, now)) {
                result.put(upper, cachedPrice.price());
                continue;
            }

            String coinId = SYMBOL_TO_COINGECKO_ID.get(upper);
            if (coinId != null) {
                symbolToIdToRefresh.put(upper, coinId);
            } else {
                log.debug("No CoinGecko mapping found for symbol {}", upper);
            }
        }

        if (!symbolToIdToRefresh.isEmpty()) {
            refreshPrices(symbolToIdToRefresh, result, now);
        }

        applyFallbackFromCache(symbols, result);

        return result;
    }

    private void refreshPrices(
            Map<String, String> symbolToIdToRefresh,
            Map<String, BigDecimal> result,
            Instant now
    ) {
        String ids = String.join(",", symbolToIdToRefresh.values());
        String url = "https://api.coingecko.com/api/v3/simple/price?ids=" + ids + "&vs_currencies=usd";

        try {
            Map<String, Map<String, Object>> response = restTemplate.getForObject(url, Map.class);

            if (response == null) {
                log.warn("CoinGecko returned null response for ids={}", ids);
                return;
            }

            for (Map.Entry<String, String> entry : symbolToIdToRefresh.entrySet()) {
                String symbol = entry.getKey();
                String coinId = entry.getValue();

                Map<String, Object> priceData = response.get(coinId);
                if (priceData != null && priceData.get("usd") != null) {
                    Object usdValue = priceData.get("usd");
                    BigDecimal price = new BigDecimal(usdValue.toString());

                    result.put(symbol, price);
                    priceCache.put(symbol, new CachedPrice(price, now));

                    log.debug("Refreshed market price for {} -> {}", symbol, price);
                }
            }
        } catch (Exception ex) {
            log.warn("Failed to fetch market prices from CoinGecko: {}", ex.getMessage());
        }
    }

    private void applyFallbackFromCache(List<String> symbols, Map<String, BigDecimal> result) {
        for (String symbol : symbols) {
            if (symbol == null || symbol.isBlank()) {
                continue;
            }

            String upper = symbol.toUpperCase(Locale.ROOT);

            if (!result.containsKey(upper)) {
                CachedPrice cachedPrice = priceCache.get(upper);
                if (cachedPrice != null) {
                    result.put(upper, cachedPrice.price());
                    log.warn("Using cached fallback price for {}", upper);
                }
            }
        }
    }

    private boolean isFresh(CachedPrice cachedPrice, Instant now) {
        return cachedPrice != null
                && cachedPrice.fetchedAt().plus(CACHE_TTL).isAfter(now);
    }

    private record CachedPrice(BigDecimal price, Instant fetchedAt) {
    }
}