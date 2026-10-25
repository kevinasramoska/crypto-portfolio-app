package com.kevinas.crypto_portfolio_backend.controller;

import com.kevinas.crypto_portfolio_backend.dto.SupportedCoinResponse;
import com.kevinas.crypto_portfolio_backend.service.MarketDataService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/market")
@RequiredArgsConstructor
public class MarketDataController {

    private final MarketDataService marketDataService;

    @GetMapping("/prices")
    public ResponseEntity<Map<String, BigDecimal>> getPrices(@RequestParam List<String> symbols) {
        if (symbols.size() > 100) throw new IllegalArgumentException("At most 100 symbols per request");
        return ResponseEntity.ok(marketDataService.getCurrentPrices(symbols));
    }

    @GetMapping("/quotes")
    public ResponseEntity<Map<String, com.kevinas.crypto_portfolio_backend.dto.MarketQuote>> getQuotes(
            @RequestParam List<String> symbols) {
        if (symbols.size() > 100) throw new IllegalArgumentException("At most 100 symbols per request");
        marketDataService.getCurrentPrices(symbols);
        Map<String, com.kevinas.crypto_portfolio_backend.dto.MarketQuote> quotes = new java.util.LinkedHashMap<>();
        symbols.forEach(symbol -> quotes.put(symbol.toUpperCase(java.util.Locale.ROOT),
                marketDataService.getPriceMetadata(symbol)));
        return ResponseEntity.ok(quotes);
    }

    @GetMapping("/supported-coins")
    public ResponseEntity<List<SupportedCoinResponse>> getSupportedCoins() {
        return ResponseEntity.ok(marketDataService.getSupportedCoins());
    }
}
