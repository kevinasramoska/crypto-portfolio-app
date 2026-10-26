import type { Holding, PortfolioSummary, Transaction } from "./types";

export const DEMO_AS_OF = "2026-10-01T12:00:00Z";
type Position = [symbol: string, name: string, quantity: number, average: number, samplePrice: number];
type Trade = [symbol: string, name: string, type: "BUY" | "SELL", quantity: number, price: number, profit: number, day: string];

function portfolio(id: string, title: string, subtitle: string, lesson: string, positions: Position[], trades: Trade[], values: number[]) {
  // Display-only fixtures: never sent to account APIs or presented as live market data.
  const holdings: Holding[] = positions.map(([symbol, name, quantity, average, samplePrice], index) => ({
    id: index + 1, symbol, name, quantity, averageBuyPriceUsd: average, currentPriceUsd: samplePrice,
    investedValueUsd: quantity * average, currentValueUsd: quantity * samplePrice,
    profitLossUsd: quantity * (samplePrice - average), marketPriceAvailable: true,
  }));
  const transactions: Transaction[] = trades.map(([symbol, name, type, quantity, price, profit, day], index) => ({
    id: index + 1, symbol, name, type, quantity, priceUsd: price, realisedProfitUsd: profit,
    totalValueUsd: quantity * price, createdAt: `${day}T12:00:00Z`, executedAt: `${day}T12:00:00Z`,
  })).reverse();
  const invested = holdings.reduce((sum, holding) => sum + holding.investedValueUsd, 0);
  const current = holdings.reduce((sum, holding) => sum + holding.currentValueUsd, 0);
  const realised = transactions.reduce((sum, trade) => sum + trade.realisedProfitUsd, 0);
  const summary: PortfolioSummary = {
    totalInvestedUsd: invested, totalCurrentValueUsd: current, totalRealisedProfitLossUsd: realised,
    totalUnrealisedProfitLossUsd: current - invested, totalProfitLossUsd: current - invested + realised,
    hasUnsupportedMarketData: false,
    holdings: holdings.map(holding => ({ ...holding, unrealisedProfitLossUsd: holding.profitLossUsd })),
  };
  const points = values.map((value, index) => ({
    x: new Date(Date.parse("2026-07-03T12:00:00Z") + index * (90 / (values.length - 1)) * 86_400_000).toISOString(),
    y1: value, y2: value - invested + realised, valid: true,
  }));
  return { id, title, subtitle, lesson, holdings, transactions, summary, points };
}

export const DEMO_PORTFOLIOS = [
  portfolio("long-term", "Long-term builder", "Accumulation · 3 assets", "Two BTC buys combine into a $42,000 weighted average. Open-position gains remain separate from realised profit.", [
    ["BTC", "Bitcoin", 0.5, 42000, 64000], ["ETH", "Ethereum", 5, 2400, 3200], ["SOL", "Solana", 100, 100, 150],
  ], [
    ["BTC", "Bitcoin", "BUY", 0.4, 40000, 0, "2026-04-03"], ["BTC", "Bitcoin", "BUY", 0.1, 50000, 0, "2026-05-10"],
    ["ETH", "Ethereum", "BUY", 5, 2400, 0, "2026-05-16"], ["SOL", "Solana", "BUY", 100, 100, 0, "2026-06-02"],
  ], [44000, 46500, 45200, 49000, 51500, 50200, 54800, 58000, 56300, 61000, 60000, 63000]),
  portfolio("active", "Active trader", "Partial exits · corrected entry", "BTC and SOL partial sells realise $6,000 while retaining their remaining cost basis. The ETH sample shows a corrected $2,500 entry; the preview lists final active records, not an interactive audit log.", [
    ["BTC", "Bitcoin", 0.3, 40000, 64000], ["SOL", "Solana", 60, 100, 150], ["ETH", "Ethereum", 4, 2500, 3200],
  ], [
    ["BTC", "Bitcoin", "BUY", 0.5, 40000, 0, "2026-03-01"], ["SOL", "Solana", "BUY", 100, 100, 0, "2026-04-01"],
    ["BTC", "Bitcoin", "SELL", 0.2, 60000, 4000, "2026-05-01"], ["SOL", "Solana", "SELL", 40, 150, 2000, "2026-05-15"],
    ["ETH", "Ethereum", "BUY", 4, 2500, 0, "2026-06-03"],
  ], [29000, 32500, 31000, 35500, 33000, 37000, 35000, 39000, 37500, 40500, 39500, 41000]),
  portfolio("drawdown", "Market drawdown", "Risk visibility · unrealised losses", "A portfolio is useful on red days too. Negative results stay visible; they are not hidden by a chart showing only winners.", [
    ["BTC", "Bitcoin", 0.25, 80000, 64000], ["ETH", "Ethereum", 4, 4000, 3200], ["DOGE", "Dogecoin", 10000, 0.2, 0.12],
  ], [
    ["BTC", "Bitcoin", "BUY", 0.25, 80000, 0, "2026-05-01"], ["ETH", "Ethereum", "BUY", 4, 4000, 0, "2026-05-08"],
    ["DOGE", "Dogecoin", "BUY", 10000, 0.2, 0, "2026-06-01"],
  ], [38000, 36500, 37200, 35000, 34500, 35500, 33000, 32000, 33500, 31500, 30500, 30000]),
];
