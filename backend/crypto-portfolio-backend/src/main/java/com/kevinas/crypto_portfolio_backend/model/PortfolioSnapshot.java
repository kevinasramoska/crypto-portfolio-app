package com.kevinas.crypto_portfolio_backend.model;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "portfolio_snapshots")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PortfolioSnapshot {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false, updatable = false)
    private Instant snapshotAt;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal totalInvestedUsd;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal totalCurrentValueUsd;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal totalProfitLossUsd;

    @PrePersist
    public void prePersist() {
        if (this.snapshotAt == null) {
            this.snapshotAt = Instant.now();
        }
    }
}
