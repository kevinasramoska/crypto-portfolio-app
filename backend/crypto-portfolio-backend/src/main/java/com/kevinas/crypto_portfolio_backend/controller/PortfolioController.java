package com.kevinas.crypto_portfolio_backend.controller;


import com.kevinas.crypto_portfolio_backend.dto.HoldingRequest;
import com.kevinas.crypto_portfolio_backend.dto.HoldingResponse;
import com.kevinas.crypto_portfolio_backend.service.PortfolioService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/portfolio")
@RequiredArgsConstructor
public class PortfolioController {

    private final PortfolioService portfolioService;

    @GetMapping("/holdings")
    public ResponseEntity<List<HoldingResponse>> getHoldings() {
        return ResponseEntity.ok(portfolioService.getUserHoldings());
    }

    @PostMapping("/holdings")
    public ResponseEntity<HoldingResponse> addHolding(
            @Valid @RequestBody HoldingRequest request
    ) {
        return ResponseEntity.ok(portfolioService.addHolding(request));
    }

    @PutMapping("/holdings/{id}")
    public ResponseEntity<HoldingResponse> updateHolding(
            @PathVariable Long id,
            @Valid @RequestBody HoldingRequest request
    ) {
        return ResponseEntity.ok(portfolioService.updateHolding(id, request));
    }

    @DeleteMapping("/holdings/{id}")
    public ResponseEntity<Void> deleteHolding(@PathVariable Long id) {
        portfolioService.deleteHolding(id);
        return ResponseEntity.noContent().build();
    }
}