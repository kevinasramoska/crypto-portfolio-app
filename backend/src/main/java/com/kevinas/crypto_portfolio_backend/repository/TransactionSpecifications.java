package com.kevinas.crypto_portfolio_backend.repository;

import com.kevinas.crypto_portfolio_backend.dto.TransactionHistoryFilter;
import com.kevinas.crypto_portfolio_backend.model.Transaction;
import com.kevinas.crypto_portfolio_backend.model.User;
import org.springframework.data.jpa.domain.Specification;
import java.util.ArrayList;
import jakarta.persistence.criteria.Predicate;

public final class TransactionSpecifications {
    private TransactionSpecifications() {}

    public static Specification<Transaction> activeHistory(User user, TransactionHistoryFilter filter) {
        return (root, query, builder) -> {
            var predicates = new ArrayList<Predicate>();
            predicates.add(builder.equal(root.get("user"), user));
            predicates.add(builder.isNull(root.get("voidedAt")));
            if (filter.symbol() != null) predicates.add(builder.equal(builder.upper(root.get("coin").get("symbol")), filter.symbol()));
            if (filter.type() != null) predicates.add(builder.equal(root.get("type"), filter.type()));
            if (filter.from() != null) predicates.add(builder.greaterThanOrEqualTo(root.get("executedAt"), filter.from()));
            if (filter.to() != null) predicates.add(builder.lessThan(root.get("executedAt"), filter.to()));
            return builder.and(predicates.toArray(Predicate[]::new));
        };
    }
}
