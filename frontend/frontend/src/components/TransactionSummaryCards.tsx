"use client";

import { TransactionSummary } from "@/lib/types";

type Props = {
  summary: TransactionSummary | null;
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

export default function TransactionSummaryCards({ summary, loading }: Props) {
  const cards = [
    {
      label: "Buy volume",
      value: formatCurrency(summary?.totalBuyVolumeUsd),
      helper: "Total purchase value",
    },
    {
      label: "Sell volume",
      value: formatCurrency(summary?.totalSellVolumeUsd),
      helper: "Total sale value",
    },
    {
      label: "Realised profit",
      value: formatCurrency(summary?.totalRealisedProfitUsd),
      helper: "Profit from sells",
    },
  ];

  if (loading) {
    return (
      <div className="grid grid-cols-1 gap-4 md:grid-cols-3">
        {cards.map(card => (
          <div key={card.label} className="h-28 rounded-xl border border-gray-800 bg-gray-900/50 animate-pulse" />
        ))}
      </div>
    );
  }

  return (
    <div className="grid grid-cols-1 gap-4 md:grid-cols-3">
      {cards.map(card => (
        <div key={card.label} className="rounded-xl border border-gray-800 bg-gray-950/50 p-5">
          <p className="text-xs uppercase tracking-wide text-gray-500">{card.label}</p>
          <p className="mt-3 text-2xl font-semibold text-white">{card.value}</p>
          <p className="mt-2 text-sm text-gray-500">{card.helper}</p>
        </div>
      ))}
    </div>
  );
}
