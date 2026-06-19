package com.kevinas.crypto_portfolio_backend.scheduler;

import com.kevinas.crypto_portfolio_backend.service.PortfolioHistoryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class PriceUpdateScheduler {

    private final PortfolioHistoryService portfolioHistoryService;

    @Scheduled(
            fixedDelayString = "${portfolio.snapshots.scheduling.fixed-delay-ms:3600000}",
            initialDelayString = "${portfolio.snapshots.scheduling.initial-delay-ms:300000}"
    )
    public void captureDailySnapshots() {
        log.debug("Running scheduled portfolio snapshot capture");
        portfolioHistoryService.captureSnapshotsForAllUsers();
    }
}
