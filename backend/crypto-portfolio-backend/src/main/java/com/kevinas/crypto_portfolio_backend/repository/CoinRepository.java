package com.kevinas.crypto_portfolio_backend.repository;

import com.kevinas.crypto_portfolio_backend.model.Coin;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CoinRepository extends JpaRepository<Coin, Long> {
    Optional<Coin> findBySymbolIgnoreCase(String symbol);
}
