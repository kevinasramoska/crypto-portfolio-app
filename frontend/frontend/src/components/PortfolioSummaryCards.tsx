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
      label: "Realised vs Unrealised",
      value: "",
      helper: "Breakdown of closed vs open P/L",
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
            {card.label === "Realised vs Unrealised" ? (
              // custom breakdown view
              <div className="mt-3">
                {summary ? (
                  (() => {
                    const realised = summary.totalRealisedProfitLossUsd ?? 0;
                    const unrealised = summary.totalUnrealisedProfitLossUsd ?? 0;
                    const absReal = Math.abs(realised);
                    const absUnreal = Math.abs(unrealised);
                    const totalAbs = absReal + absUnreal || 1;
                    const realisedPct = Math.round((absReal / totalAbs) * 100);
                    const unrealPct = 100 - realisedPct;

                    const realisedSign = realised >= 0 ? "+" : "-";
                    const unrealSign = unrealised >= 0 ? "+" : "-";

                    return (
                      <div>
                        <div className="h-3 w-full rounded-full bg-gray-900/50 overflow-hidden">
                          <div
                            className="h-full bg-emerald-500"
                            style={{ width: `${realisedPct}%` }}
                            aria-hidden
                          />
                          <div
                            className="h-full bg-violet-600"
                            style={{ width: `${unrealPct}%`, marginLeft: `-${unrealPct}%` }}
                            aria-hidden
                          />
                        </div>

                        <div className="mt-3 flex items-center justify-between text-sm text-gray-300">
                          <div className="flex items-center gap-2">
                            <span className="inline-block h-2 w-2 rounded-full bg-emerald-500" />
                            <span>Realised</span>
                            <span className="ml-2 font-semibold text-white">{realisedSign}{formatCurrency(Math.abs(realised))}</span>
                          </div>
                          <div className="flex items-center gap-2">
                            <span className="inline-block h-2 w-2 rounded-full bg-violet-600" />
                            <span>Unrealised</span>
                            <span className="ml-2 font-semibold text-white">{unrealSign}{formatCurrency(Math.abs(unrealised))}</span>
                          </div>
                        </div>
                        <div className="mt-1 text-xs text-gray-500">Shows relative contribution to total P/L</div>
                      </div>
                    );
                  })()
                ) : (
                  <p className="mt-3 text-2xl font-semibold text-white">—</p>
                )}
              </div>
            ) : (
              <>
                <p className="mt-3 text-2xl font-semibold text-white" data-testid="portfolio-card-value">
                  {card.value}
                </p>
                <p className="mt-2 text-sm text-gray-500">{card.helper}</p>
              </>
            )}
          </div>
        ))}
      </div>
    </div>
  );
}
