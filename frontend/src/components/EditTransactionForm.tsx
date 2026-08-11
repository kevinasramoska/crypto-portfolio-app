"use client";

import { FormEvent, useState } from "react";
import { Transaction, TransactionPayload, TransactionType } from "@/lib/types";

type Props = {
  transaction: Transaction;
  onSubmit: (payload: TransactionPayload) => Promise<void>;
  onCancel: () => void;
};

const SYMBOL_PATTERN = /^[A-Z0-9]{2,10}$/;

export default function EditTransactionForm({ transaction, onSubmit, onCancel }: Props) {
  const [type, setType] = useState<TransactionType>(transaction.type);
  const [symbol, setSymbol] = useState(transaction.symbol);
  const [name, setName] = useState(transaction.name);
  const [quantity, setQuantity] = useState(String(transaction.quantity));
  const [priceUsd, setPriceUsd] = useState(String(transaction.priceUsd));
  const [error, setError] = useState<string | null>(null);
  const [loading, setLoading] = useState(false);

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();

    const normalizedSymbol = symbol.trim().toUpperCase();
    const trimmedName = name.trim();
    const parsedQuantity = Number(quantity);
    const parsedPrice = Number(priceUsd);

    if (!SYMBOL_PATTERN.test(normalizedSymbol)) {
      setError("Symbol must use 2-10 letters or numbers.");
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
    } catch (caughtError) {
      setError(caughtError instanceof Error ? caughtError.message : "Unable to update transaction.");
    } finally {
      setLoading(false);
    }
  }

  return (
    <form onSubmit={handleSubmit} className="space-y-4 p-4" aria-label={`Edit ${transaction.symbol} transaction`}>
      <div className="grid gap-3 sm:grid-cols-2 lg:grid-cols-5">
        <label className="flex flex-col gap-1.5 text-xs text-gray-400">
          Type
          <select
            className="rounded border border-gray-700 bg-gray-900 px-3 py-2 text-sm text-white"
            value={type}
            onChange={event => setType(event.target.value as TransactionType)}
            disabled={loading}
          >
            <option value="BUY">Buy</option>
            <option value="SELL">Sell</option>
          </select>
        </label>
        <label className="flex flex-col gap-1.5 text-xs text-gray-400">
          Symbol
          <input
            className="rounded border border-gray-700 bg-gray-900 px-3 py-2 text-sm text-white"
            value={symbol}
            onChange={event => setSymbol(event.target.value.toUpperCase())}
            maxLength={10}
            disabled={loading}
          />
        </label>
        <label className="flex flex-col gap-1.5 text-xs text-gray-400">
          Name
          <input
            className="rounded border border-gray-700 bg-gray-900 px-3 py-2 text-sm text-white"
            value={name}
            onChange={event => setName(event.target.value)}
            maxLength={255}
            disabled={loading}
          />
        </label>
        <label className="flex flex-col gap-1.5 text-xs text-gray-400">
          Quantity
          <input
            className="rounded border border-gray-700 bg-gray-900 px-3 py-2 text-sm text-white"
            type="number"
            min="0.00000001"
            step="0.00000001"
            value={quantity}
            onChange={event => setQuantity(event.target.value)}
            disabled={loading}
          />
        </label>
        <label className="flex flex-col gap-1.5 text-xs text-gray-400">
          Price USD
          <input
            className="rounded border border-gray-700 bg-gray-900 px-3 py-2 text-sm text-white"
            type="number"
            min="0"
            step="0.01"
            value={priceUsd}
            onChange={event => setPriceUsd(event.target.value)}
            disabled={loading}
          />
        </label>
      </div>

      {error && <p role="alert" className="text-sm text-red-300">{error}</p>}

      <div className="flex justify-end gap-2">
        <button
          type="button"
          onClick={onCancel}
          disabled={loading}
          className="rounded border border-gray-700 px-3 py-2 text-sm text-gray-300 hover:border-gray-500"
        >
          Cancel
        </button>
        <button
          type="submit"
          disabled={loading}
          className="rounded bg-purple-600 px-3 py-2 text-sm font-semibold text-white hover:bg-purple-500 disabled:opacity-50"
        >
          {loading ? "Saving..." : "Save changes"}
        </button>
      </div>
    </form>
  );
}
