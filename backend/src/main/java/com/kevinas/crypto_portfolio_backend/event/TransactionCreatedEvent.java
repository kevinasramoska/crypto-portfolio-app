package com.kevinas.crypto_portfolio_backend.event;

public record TransactionCreatedEvent(Long transactionId, Long userId) {
}
