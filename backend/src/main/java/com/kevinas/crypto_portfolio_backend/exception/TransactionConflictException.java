package com.kevinas.crypto_portfolio_backend.exception;

public class TransactionConflictException extends RuntimeException {

    public TransactionConflictException(String message) {
        super(message);
    }
}
