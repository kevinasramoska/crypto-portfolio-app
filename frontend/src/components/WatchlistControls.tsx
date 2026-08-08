"use client";

import { ChangeEvent, FormEvent, useMemo, useState } from "react";

type Props = {
  watchlist: string[];
  onAdd: (symbol: string) => string | null;
  onRemove: (symbol: string) => void;
  disabled?: boolean;
};

const POPULAR_SYMBOLS = [
  "BTC",
  "ETH",
  "SOL",
  "XRP",
  "ADA",
  "DOGE",
  "AVAX",
  "LINK",
  "BNB",
  "MATIC",
  "DOT",
  "TRX",
  "ATOM",
  "LTC",
  "NEAR",
  "APT",
  "ARB",
  "FIL",
  "AAVE",
  "GRT",
  "FTM",
  "SUI",
  "OP"
];

export default function WatchlistControls({ watchlist, onAdd, onRemove, disabled }: Props) {
  const [input, setInput] = useState("");
  const [dropdownError, setDropdownError] = useState<string | null>(null);
  const [error, setError] = useState<string | null>(null);

  function handleSubmit(event: FormEvent) {
    event.preventDefault();
    const result = onAdd(input);
    if (result) {
      setError(result);
      return;
    }
    setInput("");
    setError(null);
  }

  function handleDropdownChange(event: ChangeEvent<HTMLSelectElement>) {
    const symbol = event.target.value;
    if (!symbol) {
      setDropdownError(null);
      return;
    }

    const result = onAdd(symbol);
    if (result) {
      setDropdownError(result);
    } else {
      setDropdownError(null);
    }
    // reset dropdown selection
    event.target.selectedIndex = 0;
  }

  const availableSymbols = useMemo(() => POPULAR_SYMBOLS.filter(sym => !watchlist.includes(sym)), [watchlist]);

  return (
    <div className="space-y-4 rounded-2xl border border-gray-800 bg-gray-950/70 p-5 shadow-lg shadow-black/10">
      <div className="grid gap-4 md:grid-cols-2">
        <form onSubmit={handleSubmit} className="space-y-2">
          <label className="text-xs uppercase tracking-wide text-gray-500" htmlFor="watchlist-input">
            Quick add
          </label>
          <div className="flex gap-2">
            <input
              id="watchlist-input"
              type="text"
              placeholder="BTC"
              className="w-32 rounded-lg border border-gray-800 bg-gray-900/70 px-3 py-2 text-sm text-white placeholder:text-gray-500 transition focus:border-purple-500 focus:outline-hidden focus:ring-2 focus:ring-purple-500/15"
              value={input}
              onChange={event => setInput(event.target.value)}
              disabled={disabled}
            />
            <button
              type="submit"
              disabled={disabled}
              className="rounded-lg bg-purple-600 px-4 py-2 text-xs font-semibold uppercase tracking-wide text-white transition hover:bg-purple-500 disabled:cursor-not-allowed disabled:opacity-50"
            >
              Add
            </button>
          </div>
          {error && <p className="text-sm text-red-300" role="alert">{error}</p>}
        </form>

        <div className="space-y-2">
          <label className="text-xs uppercase tracking-wide text-gray-500" htmlFor="watchlist-dropdown">
            Popular markets
          </label>
          <select
            id="watchlist-dropdown"
            className="w-full rounded-lg border border-gray-800 bg-gray-900/70 px-3 py-2 text-sm text-white transition focus:border-purple-500 focus:outline-hidden focus:ring-2 focus:ring-purple-500/15"
            onChange={handleDropdownChange}
            disabled={disabled || availableSymbols.length === 0}
            defaultValue=""
          >
            <option value="">Select a coin</option>
            {availableSymbols.map(symbol => (
              <option key={symbol} value={symbol}>
                {symbol}
              </option>
            ))}
          </select>
          {dropdownError && <p className="text-sm text-red-300" role="alert">{dropdownError}</p>}
        </div>
      </div>

      {watchlist.length > 0 && (
        <div>
          <p className="mb-2 text-xs text-gray-500">Click a symbol to remove it from your watchlist.</p>
          <div className="flex flex-wrap gap-2">
          {watchlist.map(symbol => (
            <button
              key={symbol}
              type="button"
              onClick={() => onRemove(symbol)}
              disabled={disabled}
              className="rounded-full border border-gray-800 bg-gray-900/40 px-3 py-1 text-xs font-semibold text-gray-200 transition hover:border-red-500 hover:text-red-300 disabled:cursor-not-allowed disabled:opacity-50"
              aria-label={`Remove ${symbol} from watchlist`}
            >
              {symbol}
            </button>
          ))}
          </div>
        </div>
      )}
    </div>
  );
}
