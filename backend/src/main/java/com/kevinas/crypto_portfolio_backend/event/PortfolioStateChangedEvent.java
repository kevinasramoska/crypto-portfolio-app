package com.kevinas.crypto_portfolio_backend.event;

public record PortfolioStateChangedEvent(Long transactionId, Long userId) {
}
