"use client";

import { PerformanceRange, PortfolioPerformanceHistory } from "@/lib/types";

type Props = {
  history: PortfolioPerformanceHistory | null;
  range: PerformanceRange;
  loading?: boolean;
  error?: string | null;
  onRangeChange: (range: PerformanceRange) => void;
};

const RANGES: PerformanceRange[] = ["7d", "30d", "90d"];

const currencyFormatter = new Intl.NumberFormat("en-US", {
  style: "currency",
  currency: "USD",
  maximumFractionDigits: 2,
});

function formatDate(value: string) {
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return "Unknown";
  return date.toLocaleDateString();
}

export default function PerformanceHistory({
  history,
  range,
  loading,
  error,
  onRangeChange,
}: Props) {
  return (
    <section className="space-y-4">
      <div className="flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between">
        <div>
          <h2 className="text-2xl font-semibold">Performance history</h2>
          <p className="text-sm text-gray-500">Snapshots are created after transactions.</p>
        </div>
        <div className="flex rounded-lg border border-gray-800 bg-gray-950/60 p-1">
          {RANGES.map(option => (
            <button
              key={option}
              type="button"
              onClick={() => onRangeChange(option)}
              className={
                option === range
                  ? "rounded-md bg-purple-600 px-3 py-1.5 text-sm font-semibold text-white"
                  : "rounded-md px-3 py-1.5 text-sm text-gray-400 hover:text-white"
              }
            >
              {option}
            </button>
          ))}
        </div>
      </div>

      {loading ? (
        <div className="h-40 rounded-xl border border-gray-800 bg-gray-900/50 animate-pulse" />
      ) : error ? (
        <div className="rounded-xl border border-red-500/40 bg-red-500/10 p-6 text-sm text-red-200">
          {error}
        </div>
      ) : !history?.history.length ? (
        <div className="rounded-xl border border-gray-800 bg-gray-900/60 p-6 text-sm text-gray-400">
          No performance snapshots in this range yet. Snapshots are created after transactions.
        </div>
      ) : (
        <div className="overflow-x-auto rounded-xl border border-gray-800">
          <table className="w-full min-w-[640px] table-auto">
            <thead className="bg-gray-900/80 text-left text-xs uppercase tracking-wider text-gray-400">
              <tr>
                <th className="px-6 py-3 font-semibold">Date</th>
                <th className="px-6 py-3 font-semibold text-right">Invested</th>
                <th className="px-6 py-3 font-semibold text-right">Current value</th>
                <th className="px-6 py-3 font-semibold text-right">Total P/L</th>
              </tr>
            </thead>
            <tbody className="bg-gray-950/40 text-sm text-gray-100">
              {history.history.map(point => (
                <tr key={point.snapshotAt} className="border-t border-gray-900/40">
                  <td className="px-6 py-4">{formatDate(point.snapshotAt)}</td>
                  <td className="px-6 py-4 text-right">{currencyFormatter.format(point.totalInvestedUsd)}</td>
                  <td className="px-6 py-4 text-right">{currencyFormatter.format(point.totalCurrentValueUsd)}</td>
                  <td className="px-6 py-4 text-right">{currencyFormatter.format(point.totalProfitLossUsd)}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
    </section>
  );
}
