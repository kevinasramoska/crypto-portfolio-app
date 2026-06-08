export type LoginResponse = {
  accessToken: string;
  tokenType?: string;
};

export type Holding = {
  id: number;
  symbol: string;
  name: string;
  quantity: number;
  averageBuyPriceUsd: number;
  currentPriceUsd: number;
  investedValueUsd: number;
  currentValueUsd: number;
  profitLossUsd: number;
  marketPriceAvailable: boolean;
};

export type PriceMap = Record<string, number>;

export type TransactionType = "BUY" | "SELL";

export type Transaction = {
  id: number;
  symbol: string;
  name: string;
  type: TransactionType;
  quantity: number;
  priceUsd: number;
  totalValueUsd: number;
  realisedProfitUsd: number;
  createdAt: string;
};

export type TransactionPayload = {
  symbol: string;
  name: string;
  type: TransactionType;
  quantity: number;
  priceUsd: number;
};

export type TransactionSummary = {
  totalBuyVolumeUsd: number;
  totalSellVolumeUsd: number;
  totalRealisedProfitUsd: number;
};

export type PortfolioHoldingSummary = {
  symbol: string;
  name: string;
  quantity: number;
  averageBuyPriceUsd: number;
  currentPriceUsd: number;
  investedValueUsd: number;
  currentValueUsd: number;
  unrealisedProfitLossUsd: number;
  marketPriceAvailable: boolean;
};

export type PortfolioSummary = {
  totalInvestedUsd: number;
  totalCurrentValueUsd: number;
  totalUnrealisedProfitLossUsd: number;
  totalRealisedProfitLossUsd: number;
  totalProfitLossUsd: number;
  holdings: PortfolioHoldingSummary[];
  hasUnsupportedMarketData: boolean;
};

export type PerformanceRange = "7d" | "30d" | "90d";

export type PortfolioPerformancePoint = {
  snapshotAt: string;
  totalInvestedUsd: number;
  totalCurrentValueUsd: number;
  totalProfitLossUsd: number;
};

export type PortfolioPerformanceHistory = {
  range: string;
  history: PortfolioPerformancePoint[];
};
