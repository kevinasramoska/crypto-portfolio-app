package com.kevinas.crypto_portfolio_backend.service.impl;

import com.kevinas.crypto_portfolio_backend.dto.PortfolioSummaryResponse;
import com.kevinas.crypto_portfolio_backend.model.PortfolioSnapshot;
import com.kevinas.crypto_portfolio_backend.model.User;
import com.kevinas.crypto_portfolio_backend.repository.PortfolioSnapshotRepository;
import com.kevinas.crypto_portfolio_backend.repository.TransactionRepository;
import com.kevinas.crypto_portfolio_backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.function.Function;

@Service
@RequiredArgsConstructor
public class PortfolioSnapshotWriter {
    private final UserRepository users;
    private final PortfolioSnapshotRepository snapshots;
    private final TransactionRepository transactions;

    // The supplied valuation uses already-fetched quotes: no external I/O while this lock is held.
    @Transactional(propagation = Propagation.REQUIRES_NEW, timeout = 10)
    public void write(Long userId, boolean daily, Function<User, PortfolioSummaryResponse> valuation) {
        User user = users.findByIdForUpdate(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        if (daily) {
            Instant start = Instant.now().truncatedTo(ChronoUnit.DAYS);
            if (!transactions.existsByUserAndVoidedAtIsNull(user)
                    || snapshots.existsByUserAndSnapshotAtBetween(user, start, start.plus(1, ChronoUnit.DAYS))) return;
        }
        PortfolioSummaryResponse summary = valuation.apply(user);
        snapshots.save(PortfolioSnapshot.builder().user(user)
                .totalInvestedUsd(summary.getTotalInvestedUsd())
                .totalCurrentValueUsd(summary.getTotalCurrentValueUsd())
                .totalProfitLossUsd(summary.getTotalProfitLossUsd())
                .valuationComplete(!summary.isHasUnsupportedMarketData() && !summary.isHasStaleMarketData())
                .build());
    }
}
