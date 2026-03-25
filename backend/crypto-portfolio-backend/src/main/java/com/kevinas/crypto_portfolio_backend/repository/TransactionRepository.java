package com.kevinas.crypto_portfolio_backend.repository;

import com.kevinas.crypto_portfolio_backend.model.Transaction;
import com.kevinas.crypto_portfolio_backend.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TransactionRepository extends JpaRepository<Transaction, Long> {
    List<Transaction> findByUserOrderByCreatedAtDesc(User user);
    List<Transaction> findByUserOrderByCreatedAtAsc(User user);
    List<Transaction> findByUser_IdOrderByCreatedAtAsc(Long userId);
    List<Transaction> findByUser_IdAndCoin_IdOrderByCreatedAtAsc(Long userId, Long coinId);
}