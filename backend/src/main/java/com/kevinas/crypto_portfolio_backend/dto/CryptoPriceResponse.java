package com.kevinas.crypto_portfolio_backend.dto;

import java.math.BigDecimal;
import java.util.List;

public record CryptoPriceResponse(Data data) {
    public record Data(List<Item> items) {
        public record Item(String symbol, BigDecimal value) {}
    }
}
