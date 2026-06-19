import { SupportedCoin } from "./types";

export const SUPPORTED_COIN_PRESETS: SupportedCoin[] = [
  { symbol: "BTC", name: "Bitcoin", coinGeckoId: "bitcoin" },
  { symbol: "ETH", name: "Ethereum", coinGeckoId: "ethereum" },
  { symbol: "SOL", name: "Solana", coinGeckoId: "solana" },
  { symbol: "ADA", name: "Cardano", coinGeckoId: "cardano" },
  { symbol: "XRP", name: "XRP", coinGeckoId: "ripple" },
  { symbol: "DOGE", name: "Dogecoin", coinGeckoId: "dogecoin" },
  { symbol: "DOT", name: "Polkadot", coinGeckoId: "polkadot" },
  { symbol: "AVAX", name: "Avalanche", coinGeckoId: "avalanche-2" },
  { symbol: "MATIC", name: "Polygon", coinGeckoId: "matic-network" },
  { symbol: "LINK", name: "Chainlink", coinGeckoId: "chainlink" },
  { symbol: "LTC", name: "Litecoin", coinGeckoId: "litecoin" },
  { symbol: "BNB", name: "BNB", coinGeckoId: "binancecoin" },
];
