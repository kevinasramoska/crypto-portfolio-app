package com.kevinas.crypto_portfolio_backend.repository;

import com.kevinas.crypto_portfolio_backend.model.PortfolioSnapshot;
import com.kevinas.crypto_portfolio_backend.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;

public interface PortfolioSnapshotRepository extends JpaRepository<PortfolioSnapshot, Long> {
    List<PortfolioSnapshot> findByUserAndSnapshotAtGreaterThanEqualOrderBySnapshotAtAsc(User user, Instant from);
    boolean existsByUserAndSnapshotAtBetween(User user, Instant from, Instant to);
    List<PortfolioSnapshot> findByUserAndSnapshotAtBetweenOrderBySnapshotAtAsc(User user, Instant from, Instant to);
    void deleteByUserAndSnapshotAtGreaterThanEqual(User user, Instant from);
}
