package com.kevinas.crypto_portfolio_backend.event;

import com.kevinas.crypto_portfolio_backend.service.PortfolioService;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class PortfolioSnapshotListenerTest {

    @Test
    void shouldCreateSnapshotForCommittedTransactionUser() {
        PortfolioService portfolioService = mock(PortfolioService.class);
        PortfolioSnapshotListener listener = new PortfolioSnapshotListener(portfolioService);

        listener.createSnapshotAfterTransactionCommit(new TransactionCreatedEvent(10L, 20L));

        verify(portfolioService).createSnapshotForUser(20L);
    }

    @Test
    void shouldKeepPostCommitFailureOutOfTheTransactionResponsePath() {
        PortfolioService portfolioService = mock(PortfolioService.class);
        doThrow(new IllegalStateException("snapshot persistence unavailable"))
                .when(portfolioService)
                .createSnapshotForUser(anyLong());
        PortfolioSnapshotListener listener = new PortfolioSnapshotListener(portfolioService);

        assertDoesNotThrow(() -> listener.createSnapshotAfterTransactionCommit(new TransactionCreatedEvent(10L, 20L)));

        verify(portfolioService).createSnapshotForUser(20L);
    }
}
