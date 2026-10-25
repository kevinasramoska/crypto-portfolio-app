package com.kevinas.crypto_portfolio_backend.service.impl;

import com.kevinas.crypto_portfolio_backend.model.*;
import com.kevinas.crypto_portfolio_backend.repository.PortfolioSnapshotRepository;
import com.kevinas.crypto_portfolio_backend.repository.UserRepository;
import com.kevinas.crypto_portfolio_backend.service.PortfolioHistoryService;
import com.kevinas.crypto_portfolio_backend.service.PortfolioService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class PortfolioHistoryServiceImpl implements PortfolioHistoryService {

    private final PortfolioService portfolioService;
    private final PortfolioSnapshotRepository portfolioSnapshotRepository;
    private final UserRepository userRepository;

    @Override
    public void captureSnapshotForUser(Long userId) {
        portfolioService.createDailySnapshotForUser(userId);
    }

    @Override
    public void captureSnapshotsForAllUsers() {
        long after = 0;
        while (true) {
            List<Long> ids = userRepository.findIdsAfter(after, org.springframework.data.domain.PageRequest.of(0, 100));
            if (ids.isEmpty()) return;
            for (Long id : ids) {
                try {
                    captureSnapshotForUser(id);
                } catch (RuntimeException exception) {
                    log.warn("Scheduled snapshot failed for user {}; continuing batch", id, exception);
                }
            }
            after = ids.getLast();
        }
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
