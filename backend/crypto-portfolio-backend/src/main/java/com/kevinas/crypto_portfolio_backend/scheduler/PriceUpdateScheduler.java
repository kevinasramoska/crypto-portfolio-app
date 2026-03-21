package com.kevinas.crypto_portfolio_backend.scheduler;

import com.kevinas.crypto_portfolio_backend.service.MarketDataService;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

//@Component
//@RequiredArgsConstructor
//public class PriceUpdateScheduler {
//
//    private final MarketDataService marketDataService;
//
//    // every 5 minutes
//    @Scheduled(fixedDelay = 300_000)
//    public void refreshPrices() {
//        marketDataService.refreshPriceCache();
//    }
//}
