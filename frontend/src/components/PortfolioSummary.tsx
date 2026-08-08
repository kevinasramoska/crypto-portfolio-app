"use client";

import { useMemo } from "react";
import { Holding, PriceMap } from "@/lib/types";

type Props = {
  holdings: Holding[];
  prices: PriceMap;
  loadingPrices?: boolean;
};

const currencyFormatter = new Intl.NumberFormat("en-US", {
  style: "currency",
  currency: "EUR",
  maximumFractionDigits: 2,
});

export default function PortfolioSummary({ holdings, prices, loadingPrices }: Props) {
  const summary = useMemo(() => {
    const totals = holdings.reduce(
      (acc, holding) => {
        const symbol = holding.symbol.toUpperCase();
        const price = prices[symbol];
        const value = typeof price === "number" ? price * holding.quantity : 0;

        acc.value += value;
        acc.quantity += holding.quantity;

        if (value > acc.bestValue) {
          acc.bestValue = value;
          acc.bestHolding = holding.symbol;
        }
        return acc;
      },
      { value: 0, quantity: 0, bestValue: 0, bestHolding: "" }
    );

    return {
      totalValue: totals.value,
      holdingsCount: holdings.length,
      totalQuantity: totals.quantity,
      bestHolding: totals.bestHolding,
      bestValue: totals.bestValue,
    };
  }, [holdings, prices]);

  const cards = [
    {
      label: "Portfolio value",
      value: loadingPrices ? "…" : currencyFormatter.format(summary.totalValue),
      helper: loadingPrices ? "Syncing live prices" : "Based on current market data",
    },
    {
      label: "Tracked assets",
      value: summary.holdingsCount.toString(),
      helper: summary.holdingsCount === 1 ? "Single holding" : "Unique holdings",
    },
    {
      label: "Largest position",
      value: summary.bestHolding ? summary.bestHolding : "—",
      helper: summary.bestHolding ? currencyFormatter.format(summary.bestValue) : "Add holdings to compare",
    },
  ];

  return (
    <div className="grid grid-cols-1 gap-4 sm:grid-cols-3">
      {cards.map(card => (
        <div key={card.label} className="rounded-2xl border border-gray-800 bg-gray-950/60 p-5">
          <p className="text-xs uppercase tracking-wide text-gray-500">{card.label}</p>
          <p className="mt-3 text-3xl font-semibold text-white">{card.value}</p>
          <p className="text-sm text-gray-500 mt-2">{card.helper}</p>
        </div>
      ))}
    </div>
  );
}
