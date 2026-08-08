package com.kevinas.crypto_portfolio_backend.service;

import com.kevinas.crypto_portfolio_backend.dto.SupportedCoinResponse;
import com.kevinas.crypto_portfolio_backend.util.CoinGeckoSymbolMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
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
@Slf4j
public class MarketDataServiceImpl implements MarketDataService {

    private final RestTemplate restTemplate;
    private final String cryptoApiBaseUrl;

    private static final Duration CACHE_TTL = Duration.ofSeconds(60);

    private final Map<String, CachedPrice> priceCache = new ConcurrentHashMap<>();

    public MarketDataServiceImpl(
            RestTemplate restTemplate,
            @Value("${crypto.api.base-url}") String cryptoApiBaseUrl
    ) {
        this.restTemplate = restTemplate;
        this.cryptoApiBaseUrl = normalizeBaseUrl(cryptoApiBaseUrl);
    }

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

            String coinId = CoinGeckoSymbolMapper.map(upper);
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

    @Override
    public List<SupportedCoinResponse> getSupportedCoins() {
        return CoinGeckoSymbolMapper.getSupportedCoins().stream()
                .map(coin -> new SupportedCoinResponse(coin.symbol(), coin.name(), coin.coinGeckoId()))
                .toList();
    }

    private void refreshPrices(
            Map<String, String> symbolToIdToRefresh,
            Map<String, BigDecimal> result,
            Instant now
    ) {
        String ids = String.join(",", symbolToIdToRefresh.values());
        String url = cryptoApiBaseUrl + "/simple/price?ids=" + ids + "&vs_currencies=usd";

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

    private String normalizeBaseUrl(String baseUrl) {
        if (baseUrl == null) {
            return "";
        }

        return baseUrl.replaceAll("/+$", "");
    }

    private record CachedPrice(BigDecimal price, Instant fetchedAt) {
    }
}
