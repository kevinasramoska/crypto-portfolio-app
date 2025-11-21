package com.kevinas.crypto_portfolio_backend.util;

import java.util.Map;

public class CoinGeckoSymbolMapper {
    private static final Map<String, String> SYMBOL_TO_ID = Map.ofEntries(
            Map.entry("btc", "bitcoin"),
            Map.entry("eth", "ethereum"),
            Map.entry("sol", "solana"),
            Map.entry("link", "chainlink"),
            Map.entry("doge", "dogecoin"),
            Map.entry("ada", "cardano"),
            Map.entry("xrp", "ripple"),
            Map.entry("dot", "polkadot"),
            Map.entry("matic", "matic-network"),
            Map.entry("avax", "avalanche-2"),
            Map.entry("uni", "uniswap"),
            Map.entry("atom", "cosmos"),
            Map.entry("xlm", "stellar"),
            Map.entry("ltc", "litecoin"),
            Map.entry("trx", "tron"),
            Map.entry("bch", "bitcoin-cash"),
            Map.entry("algo", "algorand"),
            Map.entry("icp", "internet-computer"),
            Map.entry("near", "near"),
            Map.entry("vet", "vechain")
    );

    public static String map(String symbol) {
        return SYMBOL_TO_ID.getOrDefault(symbol.toLowerCase(), null);
    }
}
