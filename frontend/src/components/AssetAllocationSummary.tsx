import { Holding } from "@/lib/types";

type Props = {
  holdings: Holding[];
  loading?: boolean;
  error?: string | null;
};

const currencyFormatter = new Intl.NumberFormat("en-US", {
  style: "currency",
  currency: "USD",
  maximumFractionDigits: 2,
});

const percentFormatter = new Intl.NumberFormat("en-US", {
  style: "percent",
  minimumFractionDigits: 1,
  maximumFractionDigits: 1,
});

function isPricedHolding(holding: Holding) {
  return holding.marketPriceAvailable && Number.isFinite(holding.currentValueUsd) && holding.currentValueUsd > 0;
}

export default function AssetAllocationSummary({ holdings, loading, error }: Props) {
  if (loading) {
    return (
      <div className="space-y-3">
        {[...Array(3)].map((_, index) => (
          <div key={index} className="h-16 rounded-lg border border-gray-800 bg-gray-900 animate-pulse" />
        ))}
      </div>
    );
  }

  if (error) {
    return (
      <div className="rounded-xl border border-red-500/40 bg-red-500/10 p-6 text-sm text-red-200">
        {error}
      </div>
    );
  }

  if (!holdings.length) {
    return (
      <div className="rounded-xl border border-gray-800 bg-gray-900/60 p-6 text-sm text-gray-400">
        No open holdings yet. Allocation will appear after your first buy transaction.
      </div>
    );
  }

  const pricedHoldings = holdings.filter(isPricedHolding);
  const unsupportedHoldings = holdings.filter(holding => !isPricedHolding(holding));
  const knownTotalValue = pricedHoldings.reduce((total, holding) => total + holding.currentValueUsd, 0);
  const hasKnownValue = knownTotalValue > 0;

  if (!hasKnownValue) {
    return (
      <div className="rounded-xl border border-amber-500/30 bg-amber-500/10 p-6 text-sm text-amber-100">
        Allocation is unavailable because none of your open holdings have supported market data.
      </div>
    );
  }

  return (
    <div className="space-y-3">
      <div className="rounded-xl border border-gray-800 bg-gray-950/60">
        {pricedHoldings.map(holding => {
          const allocation = holding.currentValueUsd / knownTotalValue;

          return (
            <div key={holding.id} className="border-t border-gray-900/60 p-4 first:border-t-0">
              <div className="flex items-start justify-between gap-4">
                <div>
                  <p className="font-semibold text-white">{holding.symbol}</p>
                  <p className="text-xs text-gray-500">{holding.name}</p>
                </div>
                <div className="text-right">
                  <p className="font-semibold text-white">{percentFormatter.format(allocation)}</p>
                  <p className="text-xs text-gray-500">{currencyFormatter.format(holding.currentValueUsd)}</p>
                </div>
              </div>
              <div className="mt-3 h-2 overflow-hidden rounded-full bg-gray-800">
                <div className="h-full rounded-full bg-purple-500" style={{ width: `${allocation * 100}%` }} />
              </div>
            </div>
          );
        })}
      </div>

      {unsupportedHoldings.length > 0 && (
        <div className="rounded-xl border border-amber-500/30 bg-amber-500/10 p-4 text-sm text-amber-100">
          {unsupportedHoldings.length} holding{unsupportedHoldings.length === 1 ? "" : "s"} excluded because market
          data is unavailable.
        </div>
      )}
    </div>
  );
}
