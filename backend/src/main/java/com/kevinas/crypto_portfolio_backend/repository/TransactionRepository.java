package com.kevinas.crypto_portfolio_backend.repository;

import com.kevinas.crypto_portfolio_backend.model.Transaction;
import com.kevinas.crypto_portfolio_backend.model.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface TransactionRepository extends JpaRepository<Transaction, Long> {
    @Query("select new com.kevinas.crypto_portfolio_backend.dto.TransactionSummaryResponse(" +
            "coalesce(sum(case when t.type = com.kevinas.crypto_portfolio_backend.model.TransactionType.BUY then t.quantity * t.priceUsd else 0 end), 0), " +
            "coalesce(sum(case when t.type = com.kevinas.crypto_portfolio_backend.model.TransactionType.SELL then t.quantity * t.priceUsd else 0 end), 0), " +
            "coalesce(sum(t.realisedProfitUsd), 0)) from Transaction t where t.user = :user and t.voidedAt is null")
    com.kevinas.crypto_portfolio_backend.dto.TransactionSummaryResponse aggregateActiveTotals(@Param("user") User user);
    List<Transaction> findByUserAndVoidedAtIsNullOrderByLedgerSequenceDesc(User user);
    List<Transaction> findByUserAndVoidedAtIsNullOrderByLedgerSequenceAsc(User user);
    Page<Transaction> findByUserAndVoidedAtIsNull(User user, Pageable pageable);
    boolean existsByUserAndVoidedAtIsNull(User user);
    Optional<Transaction> findByIdAndUser(Long id, User user);

    @Query("select transaction from Transaction transaction " +
            "where transaction.user = :user order by transaction.createdAt, transaction.id")
    List<Transaction> findAllRevisionsByUser(@Param("user") User user);

    @Query("select coalesce(max(transaction.ledgerSequence), 0) from Transaction transaction where transaction.user = :user")
    Long findMaximumLedgerSequence(@Param("user") User user);
}
