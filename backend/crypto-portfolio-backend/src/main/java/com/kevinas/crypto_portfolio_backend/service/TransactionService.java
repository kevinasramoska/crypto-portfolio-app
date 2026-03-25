package com.kevinas.crypto_portfolio_backend.service;

import com.kevinas.crypto_portfolio_backend.dto.TransactionRequest;
import com.kevinas.crypto_portfolio_backend.dto.TransactionResponse;
import com.kevinas.crypto_portfolio_backend.dto.TransactionSummaryResponse;

import java.util.List;

public interface TransactionService {

    /**
     * Creates a new transaction for the authenticated user.
     * Business rules enforced:
     * - BUY: Increases the user's holding position, updating average buy price.
     * - SELL: Cannot exceed available quantity in holdings.
     * - Quantities are scaled to 8 decimal places.
     * - Monetary values are scaled to 2 decimal places.
     * - Creates coin if it doesn't exist.
     *
     * @param request the transaction request containing symbol, name, type, quantity, and priceUsd
     * @return the created transaction response
     * @throws IllegalArgumentException if sell quantity exceeds holdings or unsupported type
     */
    TransactionResponse createTransaction(TransactionRequest request);

    /**
     * Retrieves all transactions for the authenticated user, ordered by creation date descending.
     *
     * @return list of transaction responses
     */
    List<TransactionResponse> getTransactionsForCurrentUser();

    /**
     * Retrieves a summary of transactions for the authenticated user, including total buy/sell volumes and realised profit.
     *
     * @return transaction summary response
     */
    TransactionSummaryResponse getTransactionSummary();
}