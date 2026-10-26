import { DEMO_PORTFOLIOS } from "./demoPortfolios";

describe("display-only demo portfolios", () => {
  it.each(DEMO_PORTFOLIOS)("reconciles sample totals and active trades for $id", portfolio => {
    const summary = portfolio.summary;
    expect(summary.totalProfitLossUsd).toBeCloseTo(summary.totalUnrealisedProfitLossUsd + summary.totalRealisedProfitLossUsd, 8);
    expect(portfolio.points.at(-1)?.y1).toBeCloseTo(summary.totalCurrentValueUsd, 8);
    expect(portfolio.points.at(-1)?.y2).toBeCloseTo(summary.totalProfitLossUsd, 8);
    for (const holding of portfolio.holdings) {
      const quantity = portfolio.transactions.filter(trade => trade.symbol === holding.symbol)
        .reduce((sum, trade) => sum + (trade.type === "BUY" ? trade.quantity : -trade.quantity), 0);
      expect(quantity).toBeCloseTo(holding.quantity, 8);
    }
    const netContributions = portfolio.transactions.reduce((sum, trade) => sum + (trade.type === "BUY" ? trade.totalValueUsd : -trade.totalValueUsd), 0);
    expect(summary.totalCurrentValueUsd - netContributions).toBeCloseTo(summary.totalProfitLossUsd, 8);
  });
});
