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

import java.time.LocalDate;
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

        LocalDate today = LocalDate.now();

        // Check if snapshot already exists for today
        if (portfolioSnapshotRepository.existsByUserIdAndSnapshotDate(userId, today)) {
            return; // Already captured
        }

        // Calculate current summary
        var summary = portfolioService.getPortfolioSummaryForUser(user);

        // Save snapshot
        PortfolioSnapshot snapshot = PortfolioSnapshot.builder()
                .userId(userId)
                .snapshotDate(today)
                .totalValueUsd(summary.getTotalCurrentValueUsd())
                .totalInvestedUsd(summary.getTotalInvestedUsd())
                .unrealizedPnlUsd(summary.getTotalUnrealisedProfitLossUsd())
                .build();

        portfolioSnapshotRepository.save(snapshot);
    }

    @Override
    public List<PortfolioSnapshot> getHistoryForCurrentUser(String range) {
        User user = getAuthenticatedUser();
        LocalDate end = LocalDate.now();
        LocalDate start;

        // Convert range → LocalDate range
        switch (range.toLowerCase()) {
            case "7d":
                start = end.minusDays(7);
                break;
            case "30d":
                start = end.minusDays(30);
                break;
            case "90d":
                start = end.minusDays(90);
                break;
            default:
                throw new IllegalArgumentException("Invalid range: " + range + ". Supported: 7d, 30d, 90d");
        }

        return portfolioSnapshotRepository.findByUserIdAndSnapshotDateBetweenOrderBySnapshotDateAsc(
                user.getId(), start, end);
    }

    private User getAuthenticatedUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String email = authentication.getName();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalStateException("Authenticated user not found"));
    }
}
