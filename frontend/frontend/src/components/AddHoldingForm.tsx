"use client";

import { FormEvent, useState } from "react";
import { createHolding, updateHolding } from "@/lib/api";
import { Holding } from "@/lib/types";

type Props = {
  holdings: Holding[];
  onSuccess?: () => void;
};

export default function AddHoldingForm({ holdings, onSuccess }: Props) {
  const [symbol, setSymbol] = useState("");
  const [quantity, setQuantity] = useState("");
  const [error, setError] = useState<string | null>(null);
  const [loading, setLoading] = useState(false);

  function resetForm() {
    setSymbol("");
    setQuantity("");
  }

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();

    const normalizedSymbol = symbol.trim().toUpperCase();
    const parsedQuantity = Number(quantity);

    if (!normalizedSymbol) {
      setError("Symbol is required.");
      return;
    }

    if (!Number.isFinite(parsedQuantity) || parsedQuantity <= 0) {
      setError("Quantity must be a positive number.");
      return;
    }

    setError(null);

    try {
      setLoading(true);
      const existing = holdings.find(h => h.symbol.toUpperCase() === normalizedSymbol);

      if (existing) {
        await updateHolding(existing.id, {
          symbol: normalizedSymbol,
          quantity: existing.quantity + parsedQuantity,
        });
      } else {
        await createHolding({ symbol: normalizedSymbol, quantity: parsedQuantity });
      }

      resetForm();
      onSuccess?.();
    } catch (err) {
      if (err instanceof Error) {
        setError(err.message);
      } else {
        setError("Unable to save holding. Please try again.");
      }
    } finally {
      setLoading(false);
    }
  }

  return (
    <form onSubmit={handleSubmit} className="bg-gray-900/60 border border-gray-800 rounded-xl p-6 space-y-4">
      <div className="flex flex-col gap-2">
        <label htmlFor="symbol" className="text-sm text-gray-400">
          Symbol
        </label>
        <input
          id="symbol"
          className="rounded-lg border border-gray-800 bg-gray-950/50 px-4 py-3 text-white placeholder:text-gray-600 focus:border-purple-500 focus:outline-none"
          placeholder="e.g. BTC"
          value={symbol}
          onChange={event => setSymbol(event.target.value)}
          autoComplete="off"
        />
      </div>

      <div className="flex flex-col gap-2">
        <label htmlFor="quantity" className="text-sm text-gray-400">
          Quantity
        </label>
        <input
          id="quantity"
          className="rounded-lg border border-gray-800 bg-gray-950/50 px-4 py-3 text-white placeholder:text-gray-600 focus:border-purple-500 focus:outline-none"
          placeholder="e.g. 0.5"
          value={quantity}
          onChange={event => setQuantity(event.target.value)}
          inputMode="decimal"
        />
      </div>

      {error && <p className="text-sm text-red-400">{error}</p>}

      <button
        type="submit"
        disabled={loading}
        className="w-full rounded-lg bg-purple-600 py-3 font-semibold text-white transition hover:bg-purple-500 disabled:cursor-not-allowed disabled:opacity-60"
      >
        {loading ? "Saving..." : "Add holding"}
      </button>
    </form>
  );
}
