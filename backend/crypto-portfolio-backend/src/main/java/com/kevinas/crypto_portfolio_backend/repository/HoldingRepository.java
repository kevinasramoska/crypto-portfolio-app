package com.kevinas.crypto_portfolio_backend.repository;

import com.kevinas.crypto_portfolio_backend.model.Holding;
import com.kevinas.crypto_portfolio_backend.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface HoldingRepository extends JpaRepository<Holding, Long> {
    // TODO: Consider deriving holdings entirely from transactions to eliminate duplication and ensure consistency.
    // For now, holdings remain as stored state, updated by TransactionService to support getUserHoldings.
    List<Holding> findByUser(User user);
    Optional<Holding> findByUserAndCoin_SymbolIgnoreCase(User user, String symbol);
}