package com.kevinas.crypto_portfolio_backend.service;

import com.kevinas.crypto_portfolio_backend.model.PortfolioSnapshot;

import java.util.List;

public interface PortfolioHistoryService {

    /**
     * Captures a daily portfolio snapshot for the specified user.
     * Calculates current portfolio values and saves to database.
     *
     * @param userId the ID of the user
     */
    void captureSnapshotForUser(Long userId);

    /**
     * Retrieves portfolio history for the current authenticated user within the specified time range.
     * Supported ranges: "1W" (1 week), "1M" (1 month), "3M" (3 months), "6M" (6 months), "1Y" (1 year), "ALL"
     *
     * @param range the time range string
     * @return list of portfolio snapshots ordered by date ascending
     */
    List<PortfolioSnapshot> getHistoryForCurrentUser(String range);
}
