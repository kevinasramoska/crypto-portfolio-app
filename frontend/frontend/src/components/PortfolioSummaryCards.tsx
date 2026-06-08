"use client";

import { PortfolioSummary } from "@/lib/types";

type Props = {
  summary: PortfolioSummary | null;
  loading?: boolean;
};

const currencyFormatter = new Intl.NumberFormat("en-US", {
  style: "currency",
  currency: "USD",
  maximumFractionDigits: 2,
});

function formatCurrency(value?: number) {
  return currencyFormatter.format(value ?? 0);
}

export default function PortfolioSummaryCards({ summary, loading }: Props) {
  const hasUnsupportedMarketData = summary?.hasUnsupportedMarketData ?? false;
  const cards = [
    {
      label: "Current value",
      value: formatCurrency(summary?.totalCurrentValueUsd),
      helper: "Live market valuation",
    },
    {
      label: "Invested",
      value: formatCurrency(summary?.totalInvestedUsd),
      helper: "Remaining cost basis",
    },
    {
      label: "Unrealised P/L",
      value: formatCurrency(summary?.totalUnrealisedProfitLossUsd),
      helper: "Open position result",
    },
    {
      label: "Realised P/L",
      value: formatCurrency(summary?.totalRealisedProfitLossUsd),
      helper: "Closed trade result",
    },
    {
      label: "Total P/L",
      value: formatCurrency(summary?.totalProfitLossUsd),
      helper: "Realised plus unrealised",
    },
    {
      label: "Assets",
      value: String(summary?.holdings.length ?? 0),
      helper: "Current open positions",
    },
  ];

  if (loading) {
    return (
      <div className="grid grid-cols-1 gap-4 md:grid-cols-3">
        {cards.map(card => (
          <div key={card.label} className="h-32 rounded-xl border border-gray-800 bg-gray-900/50 animate-pulse" />
        ))}
      </div>
    );
  }

  return (
    <div className="space-y-4">
      {hasUnsupportedMarketData && (
        <div className="rounded-xl border border-amber-500/30 bg-amber-500/10 p-4 text-sm text-amber-100">
          Some holdings have no supported market data. Current value and P/L are based on priced assets only.
        </div>
      )}

      <div className="grid grid-cols-1 gap-4 md:grid-cols-3">
        {cards.map(card => (
          <div key={card.label} className="rounded-xl border border-gray-800 bg-gray-950/60 p-5">
            <p className="text-xs uppercase tracking-wide text-gray-500">{card.label}</p>
            <p className="mt-3 text-2xl font-semibold text-white">{card.value}</p>
            <p className="mt-2 text-sm text-gray-500">{card.helper}</p>
          </div>
        ))}
      </div>
    </div>
  );
}
