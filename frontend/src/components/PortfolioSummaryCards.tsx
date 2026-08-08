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

function profitLossClass(value?: number) {
  if (!value) return "text-white";
  return value > 0 ? "text-emerald-300" : "text-red-300";
}

function signedCurrency(value: number) {
  return `${value > 0 ? "+" : value < 0 ? "-" : ""}${formatCurrency(Math.abs(value))}`;
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
      valueClassName: profitLossClass(summary?.totalUnrealisedProfitLossUsd),
    },
    {
      label: "Realised P/L",
      value: formatCurrency(summary?.totalRealisedProfitLossUsd),
      helper: "Closed trade result",
      valueClassName: profitLossClass(summary?.totalRealisedProfitLossUsd),
    },
    {
      label: "Total P/L",
      value: formatCurrency(summary?.totalProfitLossUsd),
      helper: "Realised plus unrealised",
      valueClassName: profitLossClass(summary?.totalProfitLossUsd),
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
          <div
            key={card.label}
            className="rounded-xl border border-gray-800 bg-gray-950/60 p-5"
            data-testid={`portfolio-card-${card.label.toLowerCase().replaceAll(/[^a-z0-9]+/g, "-")}`}
          >
            <p className="text-xs uppercase tracking-wide text-gray-500">{card.label}</p>
            <p
              className={`mt-3 text-2xl font-semibold ${card.valueClassName ?? "text-white"}`}
              data-testid="portfolio-card-value"
            >
              {card.value}
            </p>
            <p className="mt-2 text-sm text-gray-500">{card.helper}</p>
          </div>
        ))}
      </div>

      <ProfitLossBreakdown summary={summary} />
    </div>
  );
}

function ProfitLossBreakdown({ summary }: { summary: PortfolioSummary | null }) {
  if (!summary) {
    return (
      <div className="rounded-xl border border-gray-800 bg-gray-950/60 p-5">
        <p className="text-xs uppercase tracking-wide text-gray-500">Realised vs unrealised</p>
        <p className="mt-3 text-sm text-gray-500">Your profit and loss breakdown will appear once portfolio data is available.</p>
      </div>
    );
  }

  const realised = summary.totalRealisedProfitLossUsd ?? 0;
  const unrealised = summary.totalUnrealisedProfitLossUsd ?? 0;
  const totalAbsoluteValue = Math.abs(realised) + Math.abs(unrealised);
  const realisedWidth = totalAbsoluteValue ? (Math.abs(realised) / totalAbsoluteValue) * 100 : 50;
  const unrealisedWidth = totalAbsoluteValue ? 100 - realisedWidth : 50;

  return (
    <div className="rounded-xl border border-gray-800 bg-gray-950/60 p-5">
      <div className="flex flex-col gap-1 sm:flex-row sm:items-baseline sm:justify-between">
        <p className="text-xs uppercase tracking-wide text-gray-500">Realised vs unrealised</p>
        <p className="text-xs text-gray-500">Share of absolute profit and loss</p>
      </div>
      <div
        className="mt-4 flex h-3 overflow-hidden rounded-full bg-gray-900"
        aria-label={`Realised ${signedCurrency(realised)}. Unrealised ${signedCurrency(unrealised)}.`}
        role="img"
      >
        <div className={realised >= 0 ? "bg-emerald-500" : "bg-red-500"} style={{ width: `${realisedWidth}%` }} />
        <div className={unrealised >= 0 ? "bg-emerald-400" : "bg-red-400"} style={{ width: `${unrealisedWidth}%` }} />
      </div>
      <div className="mt-4 grid gap-3 text-sm sm:grid-cols-2">
        <div className="flex items-center justify-between gap-3 rounded-lg bg-gray-900/60 px-3 py-2">
          <span className="text-gray-400">Realised</span>
          <span className={`font-semibold ${profitLossClass(realised)}`}>{signedCurrency(realised)}</span>
        </div>
        <div className="flex items-center justify-between gap-3 rounded-lg bg-gray-900/60 px-3 py-2">
          <span className="text-gray-400">Unrealised</span>
          <span className={`font-semibold ${profitLossClass(unrealised)}`}>{signedCurrency(unrealised)}</span>
        </div>
      </div>
    </div>
  );
}
