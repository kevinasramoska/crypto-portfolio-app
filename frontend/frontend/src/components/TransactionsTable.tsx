"use client";

import { Transaction } from "@/lib/types";

type Props = {
  transactions: Transaction[];
  loading?: boolean;
  error?: string | null;
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

export default function TransactionsTable({ transactions, loading, error }: Props) {
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
        No transactions yet.
      </div>
    );
  }

  return (
    <div className="overflow-x-auto rounded-xl border border-gray-800">
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
            <tr key={transaction.id} className="border-t border-gray-900/40">
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
              <td className="px-6 py-4 text-right">{formatQuantity(transaction.quantity)}</td>
              <td className="px-6 py-4 text-right">{currencyFormatter.format(transaction.priceUsd)}</td>
              <td className="px-6 py-4 text-right">{currencyFormatter.format(transaction.totalValueUsd)}</td>
              <td className="px-6 py-4 text-right">{currencyFormatter.format(transaction.realisedProfitUsd)}</td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
}
