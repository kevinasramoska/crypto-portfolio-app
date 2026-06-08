package com.kevinas.crypto_portfolio_backend.service.impl;

import com.kevinas.crypto_portfolio_backend.model.*;
import com.kevinas.crypto_portfolio_backend.repository.PortfolioSnapshotRepository;
import com.kevinas.crypto_portfolio_backend.repository.UserRepository;
import com.kevinas.crypto_portfolio_backend.service.PortfolioHistoryService;
import com.kevinas.crypto_portfolio_backend.service.PortfolioService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PortfolioHistoryServiceImpl implements PortfolioHistoryService {

    private final PortfolioService portfolioService;
    private final PortfolioSnapshotRepository portfolioSnapshotRepository;
    private final UserRepository userRepository;

    @Override
    @Transactional
    public void captureSnapshotForUser(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        Instant startOfDay = Instant.now()
                .atZone(ZoneId.systemDefault())
                .truncatedTo(ChronoUnit.DAYS)
                .toInstant();
        Instant endOfDay = startOfDay.plus(1, ChronoUnit.DAYS);

        if (portfolioSnapshotRepository.existsByUserAndSnapshotAtBetween(user, startOfDay, endOfDay)) {
            return;
        }

        var summary = portfolioService.getPortfolioSummaryForUser(user);

        PortfolioSnapshot snapshot = PortfolioSnapshot.builder()
                .user(user)
                .totalInvestedUsd(summary.getTotalInvestedUsd())
                .totalCurrentValueUsd(summary.getTotalCurrentValueUsd())
                .totalProfitLossUsd(summary.getTotalProfitLossUsd())
                .build();

        portfolioSnapshotRepository.save(snapshot);
    }

    @Override
    public List<PortfolioSnapshot> getHistoryForCurrentUser(String range) {
        User user = getAuthenticatedUser();
        Instant end = Instant.now();
        Instant start;

        switch (range.toLowerCase()) {
            case "7d":
                start = end.minus(7, ChronoUnit.DAYS);
                break;
            case "30d":
                start = end.minus(30, ChronoUnit.DAYS);
                break;
            case "90d":
                start = end.minus(90, ChronoUnit.DAYS);
                break;
            default:
                throw new IllegalArgumentException("Invalid range: " + range + ". Supported: 7d, 30d, 90d");
        }

        return portfolioSnapshotRepository.findByUserAndSnapshotAtBetweenOrderBySnapshotAtAsc(user, start, end);
    }

    private User getAuthenticatedUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String email = authentication.getName();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalStateException("Authenticated user not found"));
    }
}
