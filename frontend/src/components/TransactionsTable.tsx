"use client";

import { Transaction } from "@/lib/types";

type Props = {
  transactions: Transaction[];
  loading?: boolean;
  error?: string | null;
  exportLoading?: boolean;
  onExportCsv?: () => void;
  pageNumber?: number;
  pageSize?: number;
  totalElements?: number;
  totalPages?: number;
  hasNext?: boolean;
  hasPrevious?: boolean;
  onPageChange?: (page: number) => void;
};

const currencyFormatter = new Intl.NumberFormat("en-US", {
  style: "currency",
  currency: "USD",
  maximumFractionDigits: 2,
});

function formatQuantity(quantity: number) {
  return quantity.toLocaleString(undefined, {
    minimumFractionDigits: 0,
    maximumFractionDigits: 8,
  });
}

function formatDate(value: string) {
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return "Unknown";
  return date.toLocaleString();
}

function profitLossClass(value: number) {
  if (value > 0) return "text-emerald-300";
  if (value < 0) return "text-red-300";
  return "text-gray-300";
}

export default function TransactionsTable({
  transactions,
  loading,
  error,
  exportLoading = false,
  onExportCsv,
  pageNumber = 0,
  pageSize = 20,
  totalElements = 0,
  totalPages = 0,
  hasNext = false,
  hasPrevious = false,
  onPageChange,
}: Props) {
  if (loading) {
    return (
      <div className="space-y-3">
        {[...Array(4)].map((_, index) => (
          <div key={index} className="h-14 rounded-lg border border-gray-800 bg-gray-900 animate-pulse" />
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

  if (!transactions.length) {
    return (
      <div className="rounded-xl border border-gray-800 bg-gray-900/60 p-6 text-sm text-gray-400">
        No transactions yet. Record a buy or sell transaction to get started.
      </div>
    );
  }

  const startingIndex = pageNumber * pageSize + 1;
  const endingIndex = Math.min((pageNumber + 1) * pageSize, totalElements);

  return (
    <div className="space-y-4">
      {onExportCsv && (
        <div className="flex justify-end">
          <button
            type="button"
            onClick={onExportCsv}
            disabled={exportLoading}
            className="rounded-lg border border-gray-700 px-3 py-1.5 text-xs uppercase tracking-wide text-gray-300 transition hover:border-purple-500 hover:text-white disabled:cursor-not-allowed disabled:opacity-50"
          >
            {exportLoading ? "Exporting..." : "Export CSV"}
          </button>
        </div>
      )}

      <div className="overflow-x-auto rounded-xl border border-gray-800 bg-gray-950/40 shadow-sm" data-testid="transactions-table">
        <table className="w-full min-w-[780px] table-auto">
          <thead className="bg-gray-900/80 text-left text-xs uppercase tracking-wider text-gray-400">
            <tr>
              <th className="px-6 py-3 font-semibold">Date</th>
              <th className="px-6 py-3 font-semibold">Type</th>
              <th className="px-6 py-3 font-semibold">Asset</th>
              <th className="px-6 py-3 font-semibold text-right">Quantity</th>
              <th className="px-6 py-3 font-semibold text-right">Price</th>
              <th className="px-6 py-3 font-semibold text-right">Total</th>
              <th className="px-6 py-3 font-semibold text-right">Realised P/L</th>
            </tr>
          </thead>
          <tbody className="bg-gray-950/40 text-sm text-gray-100">
            {transactions.map(transaction => (
              <tr
                key={transaction.id}
                className="border-t border-gray-800/70 transition-colors hover:bg-purple-500/5"
                data-testid={`transaction-row-${transaction.symbol}-${transaction.type.toLowerCase()}`}
              >
                <td className="px-6 py-4 text-gray-300">{formatDate(transaction.createdAt)}</td>
                <td className="px-6 py-4">
                  <span
                    className={
                      transaction.type === "BUY"
                        ? "rounded-full border border-emerald-500/40 px-2.5 py-1 text-xs font-semibold text-emerald-300"
                        : "rounded-full border border-amber-500/40 px-2.5 py-1 text-xs font-semibold text-amber-300"
                    }
                  >
                    {transaction.type}
                  </span>
                </td>
                <td className="px-6 py-4">
                  <div className="font-semibold">{transaction.symbol}</div>
                  <div className="text-xs text-gray-500">{transaction.name}</div>
                </td>
                <td className="px-6 py-4 text-right tabular-nums">{formatQuantity(transaction.quantity)}</td>
                <td className="px-6 py-4 text-right tabular-nums">{currencyFormatter.format(transaction.priceUsd)}</td>
                <td className="px-6 py-4 text-right tabular-nums">{currencyFormatter.format(transaction.totalValueUsd)}</td>
                <td className={`px-6 py-4 text-right font-medium tabular-nums ${profitLossClass(transaction.realisedProfitUsd)}`}>
                  {currencyFormatter.format(transaction.realisedProfitUsd)}
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>

      {onPageChange && totalElements > 0 && (
        <div className="flex items-center justify-between rounded-lg border border-gray-800 bg-gray-900/50 px-6 py-3 text-sm text-gray-300">
          <div className="text-xs text-gray-500">
            Showing {startingIndex} to {endingIndex} of {totalElements} transactions
          </div>
          <div className="flex gap-2">
            <button
              onClick={() => onPageChange(pageNumber - 1)}
              disabled={!hasPrevious}
              className="rounded border border-gray-700 px-3 py-1 text-xs uppercase tracking-wide disabled:cursor-not-allowed disabled:opacity-50 enabled:hover:border-purple-500 enabled:hover:text-white"
            >
              Previous
            </button>
            <div className="flex items-center gap-2 px-2 text-xs">
              Page {pageNumber + 1} of {totalPages}
            </div>
            <button
              onClick={() => onPageChange(pageNumber + 1)}
              disabled={!hasNext}
              className="rounded border border-gray-700 px-3 py-1 text-xs uppercase tracking-wide disabled:cursor-not-allowed disabled:opacity-50 enabled:hover:border-purple-500 enabled:hover:text-white"
            >
              Next
            </button>
          </div>
        </div>
      )}
    </div>
  );
}
