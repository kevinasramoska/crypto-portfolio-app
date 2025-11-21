package com.kevinas.crypto_portfolio_backend.service;

import com.kevinas.crypto_portfolio_backend.util.CoinGeckoSymbolMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;


import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
@Slf4j
public class MarketDataServiceImpl implements MarketDataService {

    private final WebClient.Builder webClientBuilder;

    private final Map<String, BigDecimal> priceCache = new ConcurrentHashMap<>();

    @Override
    public Map<String, BigDecimal> getCurrentPricesForSymbols(Iterable<String> symbols) {
        List<String> symbolList = new ArrayList<>();
        symbols.forEach(s -> symbolList.add(s.toLowerCase()));

        if (priceCache.isEmpty()) {
            refreshPriceCache(symbolList);
        }

        Map<String, BigDecimal> result = new HashMap<>();
        for (String s : symbolList) {
            String geckoId = com.kevinas.crypto_portfolio_backend.util.CoinGeckoSymbolMapper.map(s);
            result.put(s.toUpperCase(), priceCache.get(geckoId));
        }
        return result;
    }

    @Override
    public void refreshPriceCache() {
        refreshPriceCache(new ArrayList<>(priceCache.keySet()));
    }

    private void refreshPriceCache(List<String> symbols) {
        if (symbols.isEmpty()) return;

        List<String> geckoIds = symbols.stream()
                .map(s -> CoinGeckoSymbolMapper.map(s.toLowerCase()))
                .filter(Objects::nonNull)
                .distinct()
                .toList();

        String joinedIds = String.join(",", geckoIds);


        String url = String.format(
                "https://api.coingecko.com/api/v3/simple/price?ids=%s&vs_currencies=usd",
                joinedIds
        );

        log.info("Fetching prices for: {}", joinedIds);

        Map<String, Map<String, Double>> response = webClientBuilder.build()
                .get()
                .uri(url)
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<Map<String, Map<String, Double>>>() {})
                .block();

        if (response == null) {
            log.warn("CoinGecko returned empty response");
            return;
        }

        for (var entry : response.entrySet()) {
            String coinId = entry.getKey();
            Double price = entry.getValue().get("usd");
            if (price != null) {
                priceCache.put(coinId.toLowerCase(), BigDecimal.valueOf(price));

            }
        }

        log.info("Updated cache: {}", priceCache);
    }
}
