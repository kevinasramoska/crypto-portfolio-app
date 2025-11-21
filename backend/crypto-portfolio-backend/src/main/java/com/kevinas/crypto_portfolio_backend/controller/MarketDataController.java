package com.kevinas.crypto_portfolio_backend.controller;

import com.kevinas.crypto_portfolio_backend.service.MarketDataService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.Map;

@RestController
@RequestMapping("/api/market")
@RequiredArgsConstructor
public class MarketDataController {

    private final MarketDataService marketDataService;

    @GetMapping("/prices")
    public ResponseEntity<Map<String, BigDecimal>> getPrices(
            @RequestParam("symbols") String symbolsCsv
    ) {
        var symbols = symbolsCsv.split(",");
        return ResponseEntity.ok(
                marketDataService.getCurrentPricesForSymbols(java.util.List.of(symbols))
        );
    }
}
