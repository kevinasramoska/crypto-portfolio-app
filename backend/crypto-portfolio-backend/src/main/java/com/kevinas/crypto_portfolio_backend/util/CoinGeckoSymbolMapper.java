package com.kevinas.crypto_portfolio_backend.util;

import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class CoinGeckoSymbolMapper {

    private static final List<SupportedCoin> SUPPORTED_COINS = List.of(
            new SupportedCoin("BTC", "Bitcoin", "bitcoin"),
            new SupportedCoin("ETH", "Ethereum", "ethereum"),
            new SupportedCoin("SOL", "Solana", "solana"),
            new SupportedCoin("LINK", "Chainlink", "chainlink"),
            new SupportedCoin("DOGE", "Dogecoin", "dogecoin"),
            new SupportedCoin("ADA", "Cardano", "cardano"),
            new SupportedCoin("XRP", "XRP", "ripple"),
            new SupportedCoin("DOT", "Polkadot", "polkadot"),
            new SupportedCoin("MATIC", "Polygon", "matic-network"),
            new SupportedCoin("AVAX", "Avalanche", "avalanche-2"),
            new SupportedCoin("UNI", "Uniswap", "uniswap"),
            new SupportedCoin("ATOM", "Cosmos", "cosmos"),
            new SupportedCoin("XLM", "Stellar", "stellar"),
            new SupportedCoin("LTC", "Litecoin", "litecoin"),
            new SupportedCoin("TRX", "TRON", "tron"),
            new SupportedCoin("BCH", "Bitcoin Cash", "bitcoin-cash"),
            new SupportedCoin("ALGO", "Algorand", "algorand"),
            new SupportedCoin("ICP", "Internet Computer", "internet-computer"),
            new SupportedCoin("NEAR", "NEAR Protocol", "near"),
            new SupportedCoin("VET", "VeChain", "vechain"),
            new SupportedCoin("BNB", "BNB", "binancecoin")
    );

    private static final Map<String, String> SYMBOL_TO_ID = SUPPORTED_COINS.stream()
            .collect(java.util.stream.Collectors.toUnmodifiableMap(
                    supportedCoin -> supportedCoin.symbol().toLowerCase(Locale.ROOT),
                    SupportedCoin::coinGeckoId
            ));

    private CoinGeckoSymbolMapper() {
    }

    public static String map(String symbol) {
        if (symbol == null || symbol.isBlank()) {
            return null;
        }

        return SYMBOL_TO_ID.get(symbol.toLowerCase(Locale.ROOT));
    }

    public static List<SupportedCoin> getSupportedCoins() {
        return SUPPORTED_COINS;
    }

    public record SupportedCoin(String symbol, String name, String coinGeckoId) {
    }
}
