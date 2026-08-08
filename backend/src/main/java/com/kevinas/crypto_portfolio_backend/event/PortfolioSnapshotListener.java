package com.kevinas.crypto_portfolio_backend.event;

import com.kevinas.crypto_portfolio_backend.service.PortfolioService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
@Slf4j
public class PortfolioSnapshotListener {

    private final PortfolioService portfolioService;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void createSnapshotAfterTransactionCommit(TransactionCreatedEvent event) {
        try {
            portfolioService.createSnapshotForUser(event.userId());
        } catch (RuntimeException exception) {
            log.warn(
                    "Portfolio snapshot creation failed after committed transaction {} for user {}",
                    event.transactionId(),
                    event.userId(),
                    exception
            );
        }
    }
}
