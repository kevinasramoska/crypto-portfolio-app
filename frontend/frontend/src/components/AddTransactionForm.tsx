"use client";

import { FormEvent, useState } from "react";
import { TransactionPayload, TransactionType } from "@/lib/types";

type Props = {
  onSubmit: (payload: TransactionPayload) => Promise<void>;
  disabled?: boolean;
};

const POPULAR_NAMES: Record<string, string> = {
  BTC: "Bitcoin",
  ETH: "Ethereum",
  SOL: "Solana",
  LINK: "Chainlink",
  DOGE: "Dogecoin",
  ADA: "Cardano",
  XRP: "XRP",
  DOT: "Polkadot",
  AVAX: "Avalanche",
  MATIC: "Polygon",
  LTC: "Litecoin",
  BNB: "BNB",
};

export default function AddTransactionForm({ onSubmit, disabled }: Props) {
  const [type, setType] = useState<TransactionType>("BUY");
  const [symbol, setSymbol] = useState("");
  const [name, setName] = useState("");
  const [quantity, setQuantity] = useState("");
  const [priceUsd, setPriceUsd] = useState("");
  const [error, setError] = useState<string | null>(null);
  const [loading, setLoading] = useState(false);

  function resetForm() {
    setType("BUY");
    setSymbol("");
    setName("");
    setQuantity("");
    setPriceUsd("");
  }

  function handleSymbolChange(value: string) {
    const normalized = value.toUpperCase();
    setSymbol(normalized);
    if (!name.trim() || POPULAR_NAMES[symbol]) {
      setName(POPULAR_NAMES[normalized] ?? "");
    }
  }

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();

    const normalizedSymbol = symbol.trim().toUpperCase();
    const trimmedName = name.trim();
    const parsedQuantity = Number(quantity);
    const parsedPrice = Number(priceUsd);

    if (!normalizedSymbol) {
      setError("Symbol is required.");
      return;
    }
    if (!trimmedName) {
      setError("Name is required.");
      return;
    }
    if (!Number.isFinite(parsedQuantity) || parsedQuantity <= 0) {
      setError("Quantity must be greater than zero.");
      return;
    }
    if (!Number.isFinite(parsedPrice) || parsedPrice < 0) {
      setError("Price must be zero or greater.");
      return;
    }

    try {
      setLoading(true);
      setError(null);
      await onSubmit({
        symbol: normalizedSymbol,
        name: trimmedName,
        type,
        quantity: parsedQuantity,
        priceUsd: parsedPrice,
      });
      resetForm();
    } catch (err) {
      setError(err instanceof Error ? err.message : "Unable to save transaction.");
    } finally {
      setLoading(false);
    }
  }

  return (
    <form onSubmit={handleSubmit} className="rounded-xl border border-gray-800 bg-gray-900/60 p-6 space-y-4">
      <div>
        <p className="text-xs uppercase tracking-wide text-gray-500">New transaction</p>
        <h3 className="mt-1 text-xl font-semibold text-white">Record buy or sell</h3>
      </div>

      <div className="grid gap-4 sm:grid-cols-2">
        <label className="flex flex-col gap-2">
          <span className="text-sm text-gray-400">Type</span>
          <select
            className="rounded-lg border border-gray-800 bg-gray-950/60 px-4 py-3 text-white focus:border-purple-500 focus:outline-none"
            value={type}
            onChange={event => setType(event.target.value as TransactionType)}
            disabled={disabled || loading}
          >
            <option value="BUY">Buy</option>
            <option value="SELL">Sell</option>
          </select>
        </label>

        <label className="flex flex-col gap-2">
          <span className="text-sm text-gray-400">Symbol</span>
          <input
            className="rounded-lg border border-gray-800 bg-gray-950/60 px-4 py-3 text-white placeholder:text-gray-600 focus:border-purple-500 focus:outline-none"
            placeholder="BTC"
            value={symbol}
            onChange={event => handleSymbolChange(event.target.value)}
            disabled={disabled || loading}
            autoComplete="off"
          />
        </label>

        <label className="flex flex-col gap-2">
          <span className="text-sm text-gray-400">Name</span>
          <input
            className="rounded-lg border border-gray-800 bg-gray-950/60 px-4 py-3 text-white placeholder:text-gray-600 focus:border-purple-500 focus:outline-none"
            placeholder="Bitcoin"
            value={name}
            onChange={event => setName(event.target.value)}
            disabled={disabled || loading}
            autoComplete="off"
          />
        </label>

        <label className="flex flex-col gap-2">
          <span className="text-sm text-gray-400">Quantity</span>
          <input
            className="rounded-lg border border-gray-800 bg-gray-950/60 px-4 py-3 text-white placeholder:text-gray-600 focus:border-purple-500 focus:outline-none"
            placeholder="0.5"
            value={quantity}
            onChange={event => setQuantity(event.target.value)}
            disabled={disabled || loading}
            inputMode="decimal"
          />
        </label>

        <label className="flex flex-col gap-2 sm:col-span-2">
          <span className="text-sm text-gray-400">Price USD</span>
          <input
            className="rounded-lg border border-gray-800 bg-gray-950/60 px-4 py-3 text-white placeholder:text-gray-600 focus:border-purple-500 focus:outline-none"
            placeholder="65000"
            value={priceUsd}
            onChange={event => setPriceUsd(event.target.value)}
            disabled={disabled || loading}
            inputMode="decimal"
          />
        </label>
      </div>

      {error && <p className="text-sm text-red-400">{error}</p>}

      <button
        type="submit"
        disabled={disabled || loading}
        className="w-full rounded-lg bg-purple-600 py-3 font-semibold text-white transition hover:bg-purple-500 disabled:cursor-not-allowed disabled:opacity-60"
      >
        {loading ? "Saving..." : "Save transaction"}
      </button>
    </form>
  );
}
