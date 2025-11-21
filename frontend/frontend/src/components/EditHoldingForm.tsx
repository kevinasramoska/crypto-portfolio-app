"use client";

import { FormEvent, useEffect, useState } from "react";
import { updateHolding } from "@/lib/api";
import { Holding } from "@/lib/types";

type Props = {
  holding: Holding;
  onCancel?: () => void;
  onSuccess?: () => void;
};

export default function EditHoldingForm({ holding, onCancel, onSuccess }: Props) {
  const [quantity, setQuantity] = useState(holding.quantity.toString());
  const [error, setError] = useState<string | null>(null);
  const [loading, setLoading] = useState(false);

  useEffect(() => {
    setQuantity(holding.quantity.toString());
    setError(null);
  }, [holding]);

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    const parsedQuantity = Number(quantity);

    if (!Number.isFinite(parsedQuantity) || parsedQuantity <= 0) {
      setError("Quantity must be greater than zero.");
      return;
    }

    try {
      setError(null);
      setLoading(true);
      await updateHolding(holding.id, {
        symbol: holding.symbol,
        quantity: parsedQuantity,
      });
      onSuccess?.();
    } catch (err) {
      if (err instanceof Error) {
        setError(err.message);
      } else {
        setError("Unable to update this holding right now.");
      }
    } finally {
      setLoading(false);
    }
  }

  return (
    <form
      onSubmit={handleSubmit}
      className="bg-gray-900/70 border border-purple-500/40 rounded-xl p-6 space-y-4 shadow-lg"
    >
      <div>
        <p className="text-sm uppercase tracking-wide text-gray-500">Editing</p>
        <h3 className="text-xl font-semibold text-white mt-1">
          {holding.symbol} <span className="text-sm text-gray-500">({holding.name})</span>
        </h3>
      </div>

      <div className="flex flex-col gap-2">
        <label htmlFor="edit-quantity" className="text-sm text-gray-400">
          Quantity
        </label>
        <input
          id="edit-quantity"
          className="rounded-lg border border-gray-800 bg-gray-950/50 px-4 py-3 text-white placeholder:text-gray-600 focus:border-purple-500 focus:outline-none"
          value={quantity}
          onChange={event => setQuantity(event.target.value)}
          inputMode="decimal"
          autoFocus
        />
      </div>

      {error && <p className="text-sm text-red-400">{error}</p>}

      <div className="flex items-center justify-end gap-3 pt-2">
        <button
          type="button"
          onClick={onCancel}
          className="rounded-lg border border-gray-700 px-4 py-2 text-sm text-gray-300 hover:bg-gray-800 transition"
        >
          Cancel
        </button>
        <button
          type="submit"
          disabled={loading}
          className="rounded-lg bg-purple-600 px-5 py-2 text-sm font-semibold text-white hover:bg-purple-500 transition disabled:cursor-not-allowed disabled:opacity-60"
        >
          {loading ? "Saving…" : "Save changes"}
        </button>
      </div>
    </form>
  );
}
