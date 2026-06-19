package com.kevinas.crypto_portfolio_backend.controller;

import com.kevinas.crypto_portfolio_backend.dto.PaginatedTransactionsResponse;
import com.kevinas.crypto_portfolio_backend.dto.TransactionRequest;
import com.kevinas.crypto_portfolio_backend.dto.TransactionResponse;
import com.kevinas.crypto_portfolio_backend.dto.TransactionSummaryResponse;
import com.kevinas.crypto_portfolio_backend.service.TransactionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/transactions")
@RequiredArgsConstructor
public class TransactionController {

    private final TransactionService transactionService;

    @PostMapping
    public ResponseEntity<TransactionResponse> createTransaction(@Valid @RequestBody TransactionRequest request) {
        return ResponseEntity.ok(transactionService.createTransaction(request));
    }

    @GetMapping
    public ResponseEntity<List<TransactionResponse>> getTransactions() {
        return ResponseEntity.ok(transactionService.getTransactionsForCurrentUser());
    }

    @GetMapping("/paginated")
    public ResponseEntity<PaginatedTransactionsResponse> getTransactionsPaginated(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return ResponseEntity.ok(transactionService.getTransactionsForCurrentUserPaginated(page, size));
    }

    @GetMapping("/summary")
    public ResponseEntity<TransactionSummaryResponse> getTransactionSummary() {
        return ResponseEntity.ok(transactionService.getTransactionSummary());
    }
}