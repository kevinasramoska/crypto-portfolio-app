package com.kevinas.crypto_portfolio_backend.repository;

import com.kevinas.crypto_portfolio_backend.model.Holding;
import com.kevinas.crypto_portfolio_backend.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface HoldingRepository extends JpaRepository<Holding, Long> {
    // Current-position source of truth: holdings are stored state maintained by TransactionService.
    // Transaction history remains the audit log and realised P/L source.
    List<Holding> findByUser(User user);
    Optional<Holding> findByUserAndCoin_SymbolIgnoreCase(User user, String symbol);
}
