package com.kevinas.crypto_portfolio_backend.scheduler;

import com.kevinas.crypto_portfolio_backend.service.PortfolioHistoryService;
import org.junit.jupiter.api.Test;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class PriceUpdateSchedulerTest {

    @Test
    void captureDailySnapshots_shouldDelegateToPortfolioHistoryService() {
        PortfolioHistoryService portfolioHistoryService = mock(PortfolioHistoryService.class);
        PriceUpdateScheduler scheduler = new PriceUpdateScheduler(portfolioHistoryService);

        scheduler.captureDailySnapshots();

        verify(portfolioHistoryService).captureSnapshotsForAllUsers();
    }
}
