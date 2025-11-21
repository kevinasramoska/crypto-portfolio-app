package com.kevinas.crypto_portfolio_backend.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "coins")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Coin {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String symbol;

    @Column(nullable = false)
    private String name;
}
