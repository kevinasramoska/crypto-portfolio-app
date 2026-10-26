package com.kevinas.crypto_portfolio_backend.repository;

import com.kevinas.crypto_portfolio_backend.model.Coin;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CoinRepository extends JpaRepository<Coin, Long> {
    @org.springframework.data.jpa.repository.Modifying
    @org.springframework.data.jpa.repository.Query(value =
            "INSERT INTO coins (symbol, name) VALUES (:symbol, :symbol) ON CONFLICT DO NOTHING", nativeQuery = true)
    void insertIfAbsent(@org.springframework.data.repository.query.Param("symbol") String symbol);
    Optional<Coin> findBySymbolIgnoreCase(String symbol);
}
