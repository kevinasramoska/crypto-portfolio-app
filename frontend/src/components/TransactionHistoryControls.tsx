"use client";

import { FormEvent, useState } from "react";
import type { TransactionHistoryFilters } from "@/lib/types";

type Props = { onApply: (filters: TransactionHistoryFilters) => void };
const inputClass = "min-w-0 rounded-lg border border-gray-700 bg-gray-950 px-3 py-2 text-sm text-gray-100";

export default function TransactionHistoryControls({ onApply }: Props) {
  const [symbol, setSymbol] = useState("");
  const [type, setType] = useState("");
  const [from, setFrom] = useState("");
  const [to, setTo] = useState("");
  const [sort, setSort] = useState("executedAt");
  const [direction, setDirection] = useState("desc");
  const [error, setError] = useState<string | null>(null);

  function apply(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    if (from && to && from > to) { setError("Start date must not be after end date."); return; }
    setError(null);
    onApply({
      symbol: symbol.trim().toUpperCase() || undefined,
      type: type ? type as "BUY" | "SELL" : undefined,
      from: from ? new Date(`${from}T00:00:00Z`).toISOString() : undefined,
      to: to ? new Date(new Date(`${to}T00:00:00Z`).getTime() + 86_400_000).toISOString() : undefined,
      sort: sort as TransactionHistoryFilters["sort"], direction: direction as "asc" | "desc",
    });
  }

  function reset() {
    setSymbol(""); setType(""); setFrom(""); setTo(""); setSort("executedAt"); setDirection("desc"); setError(null);
    onApply({ sort: "executedAt", direction: "desc" });
  }

  return (
    <form aria-label="Filter transaction history" onSubmit={apply} className="rounded-xl border border-gray-800 bg-gray-900/40 p-4">
      <div className="grid gap-3 sm:grid-cols-2 lg:grid-cols-3">
        <label className="flex min-w-0 flex-col gap-1.5 text-xs text-gray-400">History symbol
          <input className={inputClass} value={symbol} maxLength={10} pattern="[A-Za-z0-9]{1,10}" placeholder="All assets" onChange={event => setSymbol(event.target.value)} />
        </label>
        <label className="flex min-w-0 flex-col gap-1.5 text-xs text-gray-400">Trade type
          <select className={inputClass} value={type} onChange={event => setType(event.target.value)}><option value="">All trades</option><option value="BUY">Buy</option><option value="SELL">Sell</option></select>
        </label>
        <label className="flex min-w-0 flex-col gap-1.5 text-xs text-gray-400">From date (UTC)
          <input className={inputClass} type="date" value={from} onChange={event => setFrom(event.target.value)} />
        </label>
        <label className="flex min-w-0 flex-col gap-1.5 text-xs text-gray-400">Through date (UTC)
          <input className={inputClass} type="date" value={to} onChange={event => setTo(event.target.value)} />
        </label>
        <label className="flex min-w-0 flex-col gap-1.5 text-xs text-gray-400">Sort history by
          <select className={inputClass} value={sort} onChange={event => setSort(event.target.value)}>
            <option value="executedAt">Execution time</option><option value="totalValueUsd">Trade value</option><option value="quantity">Quantity</option><option value="priceUsd">Unit price</option><option value="ledgerSequence">Entry order</option>
          </select>
        </label>
        <label className="flex min-w-0 flex-col gap-1.5 text-xs text-gray-400">Sort direction
          <select className={inputClass} value={direction} onChange={event => setDirection(event.target.value)}><option value="desc">Descending</option><option value="asc">Ascending</option></select>
        </label>
      </div>
      <div className="mt-4 flex flex-wrap items-center gap-3">
        <button className="rounded-lg bg-purple-600 px-4 py-2 text-sm font-semibold hover:bg-purple-500" type="submit">Apply filters</button>
        <button className="rounded-lg border border-gray-700 px-4 py-2 text-sm hover:border-purple-400" type="button" onClick={reset}>Reset filters</button>
        <p className="text-xs text-gray-400">Filters apply on the server and to CSV export. Summary totals remain portfolio-wide.</p>
      </div>
      {error && <p role="alert" className="mt-3 text-sm text-red-300">{error}</p>}
    </form>
  );
}
