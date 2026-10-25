package com.kevinas.crypto_portfolio_backend.model;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(
        name = "holdings",
        uniqueConstraints = @UniqueConstraint(
                name = "uq_holdings_user_coin",
                columnNames = {"user_id", "coin_id"}
        )
)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Holding {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "coin_id", nullable = false)
    private Coin coin;

    @Column(nullable = false, precision = 19, scale = 8)
    private BigDecimal quantity;

    @Column(nullable = false, precision = 28, scale = 12)
    private BigDecimal averageBuyPriceUsd;

    @Column(precision = 38, scale = 20)
    private BigDecimal costBasisUsd;

    @Column(length = 255)
    private String assetName;

    private Instant createdAt;
    private Instant updatedAt;
}
