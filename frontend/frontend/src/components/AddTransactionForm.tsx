"use client";

import { FormEvent, useEffect, useState } from "react";
import { getSupportedCoins } from "@/lib/api";
import { SUPPORTED_COIN_PRESETS } from "@/lib/supportedCoins";
import { SupportedCoin, TransactionPayload, TransactionType } from "@/lib/types";

type Props = {
  onSubmit: (payload: TransactionPayload) => Promise<void>;
  disabled?: boolean;
};

const CUSTOM_COIN_VALUE = "CUSTOM";
const SYMBOL_PATTERN = /^[A-Z0-9]{2,10}$/;

export default function AddTransactionForm({ onSubmit, disabled }: Props) {
  const [type, setType] = useState<TransactionType>("BUY");
  const [selectedCoin, setSelectedCoin] = useState(CUSTOM_COIN_VALUE);
  const [supportedCoins, setSupportedCoins] = useState<SupportedCoin[]>(SUPPORTED_COIN_PRESETS);
  const [symbol, setSymbol] = useState("");
  const [name, setName] = useState("");
  const [quantity, setQuantity] = useState("");
  const [priceUsd, setPriceUsd] = useState("");
  const [error, setError] = useState<string | null>(null);
  const [loading, setLoading] = useState(false);

  useEffect(() => {
    let cancelled = false;

    async function loadSupportedCoins() {
      try {
        const coins = await getSupportedCoins();
        if (!cancelled && coins.length > 0) {
          setSupportedCoins(coins);
        }
      } catch {
        if (!cancelled) {
          setSupportedCoins(SUPPORTED_COIN_PRESETS);
        }
      }
    }

    void loadSupportedCoins();

    return () => {
      cancelled = true;
    };
  }, []);

  function resetForm() {
    setType("BUY");
    setSelectedCoin(CUSTOM_COIN_VALUE);
    setSymbol("");
    setName("");
    setQuantity("");
    setPriceUsd("");
  }

  function handleCoinSelect(value: string) {
    setSelectedCoin(value);

    if (value === CUSTOM_COIN_VALUE) {
      setSymbol("");
      setName("");
      return;
    }

    const preset = supportedCoins.find(coin => coin.symbol === value);
    if (preset) {
      setSymbol(preset.symbol);
      setName(preset.name);
    }
  }

  function handleSymbolChange(value: string) {
    setSelectedCoin(CUSTOM_COIN_VALUE);
    setSymbol(value.toUpperCase());
  }

  function handleNameChange(value: string) {
    setSelectedCoin(CUSTOM_COIN_VALUE);
    setName(value);
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
      resetForm();
    } catch (err) {
      setError(err instanceof Error ? err.message : "Unable to save transaction.");
    } finally {
      setLoading(false);
    }
  }

  return (
    <form onSubmit={handleSubmit} noValidate className="space-y-5 rounded-2xl border border-gray-800 bg-gray-950/70 p-5 shadow-lg shadow-black/10 sm:p-6">
      <div>
        <p className="text-xs font-semibold uppercase tracking-[0.16em] text-purple-300">New transaction</p>
        <h3 className="mt-1 text-xl font-semibold text-white">Record buy or sell</h3>
        <p className="mt-1 text-sm text-gray-500">Choose a supported asset or enter one manually.</p>
      </div>

      <div className="grid gap-4 sm:grid-cols-2">
        <label className="flex flex-col gap-2">
          <span className="text-sm text-gray-400">Type</span>
          <select
            className="rounded-lg border border-gray-800 bg-gray-900/70 px-4 py-3 text-white transition focus:border-purple-500 focus:outline-hidden focus:ring-2 focus:ring-purple-500/15"
            value={type}
            onChange={event => setType(event.target.value as TransactionType)}
            disabled={disabled || loading}
          >
            <option value="BUY">Buy</option>
            <option value="SELL">Sell</option>
          </select>
        </label>

        <label className="flex flex-col gap-2">
          <span className="text-sm text-gray-400">Coin</span>
          <select
            className="rounded-lg border border-gray-800 bg-gray-900/70 px-4 py-3 text-white transition focus:border-purple-500 focus:outline-hidden focus:ring-2 focus:ring-purple-500/15"
            value={selectedCoin}
            onChange={event => handleCoinSelect(event.target.value)}
            disabled={disabled || loading}
          >
            <option value={CUSTOM_COIN_VALUE}>Custom / manual entry</option>
            {supportedCoins.map(coin => (
              <option key={coin.symbol} value={coin.symbol}>
                {coin.symbol} - {coin.name}
              </option>
            ))}
          </select>
        </label>

        <label className="flex flex-col gap-2">
          <span className="text-sm text-gray-400">Symbol</span>
          <input
            className="rounded-lg border border-gray-800 bg-gray-900/70 px-4 py-3 text-white placeholder:text-gray-600 transition focus:border-purple-500 focus:outline-hidden focus:ring-2 focus:ring-purple-500/15"
            placeholder="BTC"
            value={symbol}
            onChange={event => handleSymbolChange(event.target.value)}
            disabled={disabled || loading}
            autoComplete="off"
            maxLength={10}
            required
          />
        </label>

        <label className="flex flex-col gap-2">
          <span className="text-sm text-gray-400">Name</span>
          <input
            className="rounded-lg border border-gray-800 bg-gray-900/70 px-4 py-3 text-white placeholder:text-gray-600 transition focus:border-purple-500 focus:outline-hidden focus:ring-2 focus:ring-purple-500/15"
            placeholder="Bitcoin"
            value={name}
            onChange={event => handleNameChange(event.target.value)}
            disabled={disabled || loading}
            autoComplete="off"
            maxLength={255}
            required
          />
        </label>

        <label className="flex flex-col gap-2">
          <span className="text-sm text-gray-400">Quantity</span>
          <input
            className="rounded-lg border border-gray-800 bg-gray-900/70 px-4 py-3 text-white placeholder:text-gray-600 transition focus:border-purple-500 focus:outline-hidden focus:ring-2 focus:ring-purple-500/15"
            placeholder="0.5"
            type="number"
            min="0.00000001"
            step="0.00000001"
            value={quantity}
            onChange={event => setQuantity(event.target.value)}
            disabled={disabled || loading}
            inputMode="decimal"
            required
          />
        </label>

        <label className="flex flex-col gap-2 sm:col-span-2">
          <span className="text-sm text-gray-400">Price USD</span>
          <input
            className="rounded-lg border border-gray-800 bg-gray-900/70 px-4 py-3 text-white placeholder:text-gray-600 transition focus:border-purple-500 focus:outline-hidden focus:ring-2 focus:ring-purple-500/15"
            placeholder="65000"
            type="number"
            min="0"
            step="0.01"
            value={priceUsd}
            onChange={event => setPriceUsd(event.target.value)}
            disabled={disabled || loading}
            inputMode="decimal"
            required
          />
        </label>
      </div>

      {error && <p className="rounded-lg border border-red-500/25 bg-red-500/10 px-3 py-2 text-sm text-red-200" role="alert">{error}</p>}

      <button
        type="submit"
        disabled={disabled || loading}
        className="w-full rounded-lg bg-purple-600 py-3 font-semibold text-white shadow-lg shadow-purple-950/30 transition hover:bg-purple-500 hover:shadow-purple-900/40 disabled:cursor-not-allowed disabled:opacity-60"
      >
        {loading ? "Saving..." : "Save transaction"}
      </button>
    </form>
  );
}
