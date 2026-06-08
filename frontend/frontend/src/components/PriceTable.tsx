import { Holding } from "@/lib/types";

type Props = {
  holdings: Holding[];
  loading?: boolean;
  error?: string | null;
  emptyMessage?: string;
};

const currencyFormatter = new Intl.NumberFormat("en-US", {
  style: "currency",
  currency: "USD",
  maximumFractionDigits: 2,
});

function formatQuantity(quantity: number) {
  const formatted = quantity.toLocaleString(undefined, {
    minimumFractionDigits: 0,
    maximumFractionDigits: 12,
  });

  if (formatted === "0" && quantity !== 0) {
    return quantity.toPrecision(6);
  }

  return formatted;
}

function formatMarketValue(holding: Holding, value: number, unavailableLabel: string) {
  if (!holding.marketPriceAvailable) {
    return <span className="text-amber-300">{unavailableLabel}</span>;
  }

  return currencyFormatter.format(value);
}

export default function PriceTable({
  holdings,
  loading,
  error,
  emptyMessage = "You haven't added any holdings yet.",
}: Props) {
  if (loading) {
    return (
      <div className="mt-10 space-y-3">
        {[...Array(3)].map((_, index) => (
          <div key={index} className="h-14 rounded-lg bg-gray-900 border border-gray-800 animate-pulse" />
        ))}
      </div>
    );
  }

  if (error) {
    return (
      <div className="mt-10 rounded-xl border border-red-500/40 bg-red-500/10 p-6 text-sm text-red-200">
        {error}
      </div>
    );
  }

  if (!holdings.length) {
    return (
      <div className="mt-10 rounded-xl border border-gray-800 bg-gray-900/60 p-6 text-sm text-gray-400">
        {emptyMessage}
      </div>
    );
  }

  const hasUnsupportedMarketData = holdings.some(holding => !holding.marketPriceAvailable);
  const totalValue = holdings
    .filter(holding => holding.marketPriceAvailable)
    .reduce((acc, holding) => acc + holding.currentValueUsd, 0);

  return (
    <div className="mt-10 space-y-3">
      {hasUnsupportedMarketData && (
        <div className="rounded-xl border border-amber-500/30 bg-amber-500/10 p-4 text-sm text-amber-100">
          Some holdings do not have supported market data. Current value and P/L totals only include priced assets.
        </div>
      )}

      <div className="overflow-x-auto rounded-xl border border-gray-800">
        <table className="w-full min-w-[860px] table-auto">
          <thead className="bg-gray-900/80 text-left text-xs uppercase tracking-wider text-gray-400">
            <tr>
              <th className="px-6 py-3 font-semibold">Asset</th>
              <th className="px-6 py-3 font-semibold">Quantity</th>
              <th className="px-6 py-3 font-semibold text-right">Average buy</th>
              <th className="px-6 py-3 font-semibold text-right">Current price</th>
              <th className="px-6 py-3 font-semibold text-right">Invested</th>
              <th className="px-6 py-3 font-semibold text-right">Value</th>
              <th className="px-6 py-3 font-semibold text-right">P/L</th>
            </tr>
          </thead>
          <tbody className="bg-gray-950/40 text-sm text-gray-100">
            {holdings.map(holding => (
              <tr key={holding.id} className="border-t border-gray-900/40">
                <td className="px-6 py-4">
                  <div className="font-semibold">{holding.symbol}</div>
                  <div className="text-xs text-gray-500">{holding.name}</div>
                </td>
                <td className="px-6 py-4">{formatQuantity(holding.quantity)}</td>
                <td className="px-6 py-4 text-right">{currencyFormatter.format(holding.averageBuyPriceUsd)}</td>
                <td className="px-6 py-4 text-right">
                  {formatMarketValue(holding, holding.currentPriceUsd, "No market data")}
                </td>
                <td className="px-6 py-4 text-right">{currencyFormatter.format(holding.investedValueUsd)}</td>
                <td className="px-6 py-4 text-right">
                  {formatMarketValue(holding, holding.currentValueUsd, "Not valued")}
                </td>
                <td className="px-6 py-4 text-right">
                  {formatMarketValue(holding, holding.profitLossUsd, "Unavailable")}
                </td>
              </tr>
            ))}
          </tbody>
          <tfoot className="bg-gray-900/80 text-sm font-semibold text-gray-100">
            <tr>
              <td className="px-6 py-4" colSpan={5}>
                {hasUnsupportedMarketData ? "Known portfolio value" : "Total portfolio value"}
              </td>
              <td className="px-6 py-4 text-right">{currencyFormatter.format(totalValue)}</td>
              <td className="px-6 py-4" />
            </tr>
          </tfoot>
        </table>
      </div>
    </div>
  );
}
