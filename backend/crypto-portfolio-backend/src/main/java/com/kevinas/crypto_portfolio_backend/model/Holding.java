package com.kevinas.crypto_portfolio_backend.model;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "holdings")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Holding {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    private User user;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    private Coin coin;

    @Column(nullable = false, precision = 19, scale = 8)
    private BigDecimal quantity;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal averageBuyPriceUsd;

    private Instant createdAt;
    private Instant updatedAt;
}