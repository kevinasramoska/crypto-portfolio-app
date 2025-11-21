package com.kevinas.crypto_portfolio_backend.config;

import com.kevinas.crypto_portfolio_backend.service.MarketDataService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

@Configuration
@EnableScheduling
@RequiredArgsConstructor
public class SchedulerConfig {

    private final MarketDataService marketDataService;

    @Scheduled(fixedRate = 60000)
    public void refreshCache() {
        marketDataService.refreshPriceCache();
    }
}
