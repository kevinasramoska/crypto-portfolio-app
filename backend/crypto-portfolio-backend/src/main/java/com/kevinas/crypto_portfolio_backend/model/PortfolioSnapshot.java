package com.kevinas.crypto_portfolio_backend.model;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

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

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "snapshot_date", nullable = false)
    private LocalDate snapshotDate;

    @Column(name = "total_value_usd", nullable = false, precision = 19, scale = 2)
    private BigDecimal totalValueUsd;

    @Column(name = "total_invested_usd", nullable = false, precision = 19, scale = 2)
    private BigDecimal totalInvestedUsd;

    @Column(name = "unrealized_pnl_usd", nullable = false, precision = 19, scale = 2)
    private BigDecimal unrealizedPnlUsd;
}
