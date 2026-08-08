package com.kevinas.crypto_portfolio_backend.dto;

import java.util.List;

public record PaginatedTransactionsResponse(
        List<TransactionResponse> content,
        int pageNumber,
        int pageSize,
        long totalElements,
        int totalPages,
        boolean hasNext,
        boolean hasPrevious
) {}

