package com.kevinas.crypto_portfolio_backend.dto;

import com.kevinas.crypto_portfolio_backend.model.TransactionType;
import java.time.Instant;
import java.util.Locale;
import java.util.Set;

/** Date interval is UTC: inclusive from, exclusive to. Sorting is strictly allowlisted. */
public record TransactionHistoryFilter(String symbol, TransactionType type, Instant from, Instant to,
                                       String sort, String direction) {
    public TransactionHistoryFilter {
        symbol = symbol == null || symbol.isBlank() ? null : symbol.trim().toUpperCase(Locale.ROOT);
        if (symbol != null && !symbol.matches("[A-Z0-9]{1,10}")) throw new IllegalArgumentException("Invalid symbol filter");
        if (from != null && to != null && !from.isBefore(to)) throw new IllegalArgumentException("From must be before to");
        sort = sort == null || sort.isBlank() ? "ledgerSequence" : sort;
        if (!Set.of("ledgerSequence", "executedAt", "totalValueUsd", "quantity", "priceUsd").contains(sort)) {
            throw new IllegalArgumentException("Unsupported transaction sort");
        }
        direction = direction == null || direction.isBlank() ? "desc" : direction.toLowerCase(Locale.ROOT);
        if (!Set.of("asc", "desc").contains(direction)) throw new IllegalArgumentException("Direction must be asc or desc");
    }

    public static TransactionHistoryFilter empty() {
        return new TransactionHistoryFilter(null, null, null, null, null, null);
    }
}
