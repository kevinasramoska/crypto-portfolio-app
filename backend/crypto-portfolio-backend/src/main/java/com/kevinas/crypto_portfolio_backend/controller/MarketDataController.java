package com.kevinas.crypto_portfolio_backend.controller;

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
        return ResponseEntity.ok(marketDataService.getCurrentPrices(symbols));
    }
}