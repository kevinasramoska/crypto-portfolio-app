package com.kevinas.crypto_portfolio_backend.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

@Entity
@Table(name = "transactions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Transaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    @NotNull
    private User user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "coin_id", nullable = false)
    @NotNull
    private Coin coin;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    @NotNull
    private TransactionType type;

    @Column(nullable = false, precision = 19, scale = 8)
    @NotNull
    @Positive
    private BigDecimal quantity;

    @Column(nullable = false, precision = 19, scale = 2)
    @NotNull
    @DecimalMin("0.00")
    private BigDecimal priceUsd;

    @Column(nullable = false, precision = 19, scale = 2)
    @NotNull
    @DecimalMin("0.00")
    private BigDecimal totalValueUsd;

    @Column(nullable = false, precision = 19, scale = 2)
    @NotNull
    private BigDecimal realisedProfitUsd;

    @Column(name = "ledger_sequence", nullable = false, updatable = false)
    @NotNull
    @Positive
    private Long ledgerSequence;

    @Column(name = "voided_at")
    private Instant voidedAt;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "replacement_transaction_id")
    private Transaction replacementTransaction;

    @Column(nullable = false, updatable = false)
    @NotNull
    private Instant createdAt;

    @PrePersist
    public void prePersist() {
        if (this.createdAt == null) {
            this.createdAt = Instant.now().truncatedTo(ChronoUnit.MICROS);
        }
        if (this.totalValueUsd == null) {
            this.totalValueUsd = BigDecimal.ZERO;
        }
        if (this.realisedProfitUsd == null) {
            this.realisedProfitUsd = BigDecimal.ZERO;
        }
    }
}
