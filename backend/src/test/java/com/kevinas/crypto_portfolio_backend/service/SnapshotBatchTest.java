package com.kevinas.crypto_portfolio_backend.service;

import com.kevinas.crypto_portfolio_backend.repository.*;
import com.kevinas.crypto_portfolio_backend.service.impl.PortfolioHistoryServiceImpl;
import org.junit.jupiter.api.Test;
import java.util.List;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;

class SnapshotBatchTest {
    @Test
    void pagesUsersAndContinuesAfterOneFailure() {
        var portfolio = mock(PortfolioService.class);
        var users = mock(UserRepository.class);
        var history = new PortfolioHistoryServiceImpl(portfolio, mock(PortfolioSnapshotRepository.class), users);
        when(users.findIdsAfter(eq(0L), any())).thenReturn(List.of(1L, 2L));
        when(users.findIdsAfter(eq(2L), any())).thenReturn(List.of(3L));
        when(users.findIdsAfter(eq(3L), any())).thenReturn(List.of());
        doThrow(new IllegalStateException("isolated failure")).when(portfolio).createDailySnapshotForUser(1L);
        history.captureSnapshotsForAllUsers();
        verify(portfolio).createDailySnapshotForUser(1L);
        verify(portfolio).createDailySnapshotForUser(2L);
        verify(portfolio).createDailySnapshotForUser(3L);
    }
}
