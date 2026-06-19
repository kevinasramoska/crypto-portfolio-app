package com.kevinas.crypto_portfolio_backend.service;

import com.kevinas.crypto_portfolio_backend.dto.PortfolioSummaryResponse;
import com.kevinas.crypto_portfolio_backend.model.User;
import com.kevinas.crypto_portfolio_backend.repository.PortfolioSnapshotRepository;
import com.kevinas.crypto_portfolio_backend.repository.TransactionRepository;
import com.kevinas.crypto_portfolio_backend.repository.UserRepository;
import com.kevinas.crypto_portfolio_backend.service.impl.PortfolioHistoryServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PortfolioHistoryServiceImplTest {

    @Mock
    private PortfolioService portfolioService;

    @Mock
    private PortfolioSnapshotRepository portfolioSnapshotRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private TransactionRepository transactionRepository;

    @InjectMocks
    private PortfolioHistoryServiceImpl portfolioHistoryService;

    @Test
    void captureSnapshotForUser_shouldSaveSnapshot_whenUserHasTransactionsAndNoSnapshotToday() {
        User user = buildUser(1L, "snapshot@example.com");
        PortfolioSummaryResponse summary = PortfolioSummaryResponse.builder()
                .totalInvestedUsd(new BigDecimal("100.00"))
                .totalCurrentValueUsd(new BigDecimal("120.00"))
                .totalProfitLossUsd(new BigDecimal("20.00"))
                .build();

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(transactionRepository.existsByUser(user)).thenReturn(true);
        when(portfolioSnapshotRepository.existsByUserAndSnapshotAtBetween(
                org.mockito.ArgumentMatchers.eq(user),
                org.mockito.ArgumentMatchers.any(Instant.class),
                org.mockito.ArgumentMatchers.any(Instant.class)
        )).thenReturn(false);
        when(portfolioService.getPortfolioSummaryForUser(user)).thenReturn(summary);

        portfolioHistoryService.captureSnapshotForUser(1L);

        ArgumentCaptor<com.kevinas.crypto_portfolio_backend.model.PortfolioSnapshot> captor =
                ArgumentCaptor.forClass(com.kevinas.crypto_portfolio_backend.model.PortfolioSnapshot.class);
        verify(portfolioSnapshotRepository).save(captor.capture());

        var savedSnapshot = captor.getValue();
        assertSame(user, savedSnapshot.getUser());
        assertEquals(new BigDecimal("100.00"), savedSnapshot.getTotalInvestedUsd());
        assertEquals(new BigDecimal("120.00"), savedSnapshot.getTotalCurrentValueUsd());
        assertEquals(new BigDecimal("20.00"), savedSnapshot.getTotalProfitLossUsd());
    }

    @Test
    void captureSnapshotsForAllUsers_shouldSkipUsersWithoutTransactions() {
        User withTransactions = buildUser(1L, "active@example.com");
        User withoutTransactions = buildUser(2L, "inactive@example.com");
        PortfolioSummaryResponse summary = PortfolioSummaryResponse.builder()
                .totalInvestedUsd(new BigDecimal("50.00"))
                .totalCurrentValueUsd(new BigDecimal("60.00"))
                .totalProfitLossUsd(new BigDecimal("10.00"))
                .build();

        when(userRepository.findAll()).thenReturn(List.of(withTransactions, withoutTransactions));
        when(transactionRepository.existsByUser(withTransactions)).thenReturn(true);
        when(transactionRepository.existsByUser(withoutTransactions)).thenReturn(false);
        when(portfolioSnapshotRepository.existsByUserAndSnapshotAtBetween(
                org.mockito.ArgumentMatchers.eq(withTransactions),
                org.mockito.ArgumentMatchers.any(Instant.class),
                org.mockito.ArgumentMatchers.any(Instant.class)
        )).thenReturn(false);
        when(portfolioService.getPortfolioSummaryForUser(withTransactions)).thenReturn(summary);

        portfolioHistoryService.captureSnapshotsForAllUsers();

        verify(portfolioService).getPortfolioSummaryForUser(withTransactions);
        verify(portfolioService, never()).getPortfolioSummaryForUser(withoutTransactions);
        verify(portfolioSnapshotRepository).save(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void captureSnapshotForUser_shouldSkipWhenSnapshotAlreadyExistsToday() {
        User user = buildUser(1L, "snapshot@example.com");
        Instant startOfDay = Instant.now()
                .atZone(ZoneId.systemDefault())
                .truncatedTo(ChronoUnit.DAYS)
                .toInstant();
        Instant endOfDay = startOfDay.plus(1, ChronoUnit.DAYS);

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(transactionRepository.existsByUser(user)).thenReturn(true);
        when(portfolioSnapshotRepository.existsByUserAndSnapshotAtBetween(user, startOfDay, endOfDay)).thenReturn(true);

        portfolioHistoryService.captureSnapshotForUser(1L);

        verify(portfolioService, never()).getPortfolioSummaryForUser(user);
        verify(portfolioSnapshotRepository, never()).save(org.mockito.ArgumentMatchers.any());
    }

    private User buildUser(Long id, String email) {
        User user = new User();
        user.setId(id);
        user.setEmail(email);
        return user;
    }
}
