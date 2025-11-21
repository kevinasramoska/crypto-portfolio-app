"use client";

import { useCallback, useEffect, useMemo, useState } from "react";
import AddHoldingForm from "@/components/AddHoldingForm";
import CryptoCard from "@/components/CryptoCard";
import EditHoldingForm from "@/components/EditHoldingForm";
import HoldingsControls, { type SortKey } from "@/components/HoldingsControls";
import WatchlistControls from "@/components/WatchlistControls";
import PortfolioSummary from "@/components/PortfolioSummary";
import PriceTable from "@/components/PriceTable";
import { deleteHolding, getHoldings, getPrices } from "@/lib/api";
import { Holding, PriceMap } from "@/lib/types";

const DEFAULT_WATCHLIST = ["BTC", "ETH", "SOL", "LINK", "DOGE"];
const WATCHLIST_STORAGE_KEY = "crypto-dashboard-watchlist";

export default function DashboardPage() {
  const [prices, setPrices] = useState<PriceMap>({});
  const [priceLoading, setPriceLoading] = useState(true);
  const [watchlist, setWatchlist] = useState<string[]>(DEFAULT_WATCHLIST);
  const [holdings, setHoldings] = useState<Holding[]>([]);
  const [portfolioLoading, setPortfolioLoading] = useState(true);
  const [portfolioError, setPortfolioError] = useState<string | null>(null);
  const [portfolioRequiresAuth, setPortfolioRequiresAuth] = useState(false);
  const [deletingHoldingId, setDeletingHoldingId] = useState<number | null>(null);
  const [editingHolding, setEditingHolding] = useState<Holding | null>(null);
  const [symbolFilter, setSymbolFilter] = useState("");
  const [sortKey, setSortKey] = useState<SortKey>("alphabetical");
  const [watchlistHydrated, setWatchlistHydrated] = useState(false);
  const [priceError, setPriceError] = useState<string | null>(null);
  const [lastPriceUpdated, setLastPriceUpdated] = useState<Date | null>(null);

  const displayHoldings = useMemo(() => {
    const normalizedFilter = symbolFilter.trim().toLowerCase();
    let filtered = holdings;
    if (normalizedFilter) {
      filtered = holdings.filter(
        holding =>
          holding.symbol.toLowerCase().includes(normalizedFilter) ||
          holding.name?.toLowerCase().includes(normalizedFilter)
      );
    }

    const sorted = [...filtered];
    sorted.sort((a, b) => {
      if (sortKey === "alphabetical") {
        return a.symbol.localeCompare(b.symbol);
      }
      if (sortKey === "quantity") {
        return b.quantity - a.quantity;
      }
      const priceA = prices[a.symbol.toUpperCase()] ?? 0;
      const priceB = prices[b.symbol.toUpperCase()] ?? 0;
      const valueA = priceA * a.quantity;
      const valueB = priceB * b.quantity;
      return valueB - valueA;
    });

    return sorted;
  }, [holdings, symbolFilter, sortKey, prices]);

  const hasWatchlist = watchlist.length > 0;
  const formattedLastPriceUpdate = lastPriceUpdated?.toLocaleTimeString() ?? null;

  const emptyMessage =
    holdings.length === 0
      ? "You haven't added any holdings yet."
      : "No holdings match your current filters.";
  const isFiltering = symbolFilter.trim().length > 0;

  const handleAddWatchlist = useCallback(
    (input: string) => {
      const normalized = input.trim().toUpperCase();
      if (!normalized) return "Enter a symbol to add.";
      if (!/^[A-Z0-9]{2,10}$/.test(normalized)) return "Only letters/numbers, 2-10 chars.";
      if (watchlist.includes(normalized)) return `${normalized} is already in your watchlist.`;
      setWatchlist(prev => [...prev, normalized]);
      return null;
    },
    [watchlist]
  );

  const handleRemoveWatchlist = useCallback((symbol: string) => {
    setWatchlist(prev => prev.filter(item => item !== symbol));
  }, []);

  const loadPrices = useCallback(async () => {
    if (!watchlist.length) {
      setPrices({});
      setPriceLoading(false);
      setPriceError(null);
      setLastPriceUpdated(null);
      return;
    }

    try {
      setPriceLoading(true);
      setPriceError(null);
      const data = await getPrices(watchlist);
      setPrices(data);
      setLastPriceUpdated(new Date());
    } catch (error) {
      console.error("Error loading prices", error);
      setPriceError("Unable to fetch latest prices. Please try again.");
    } finally {
      setPriceLoading(false);
    }
  }, [watchlist]);

  const handleManualRefresh = useCallback(() => {
    loadPrices();
  }, [loadPrices]);

  const loadHoldings = useCallback(async () => {
    try {
      setPortfolioLoading(true);
      const data = await getHoldings();
      setHoldings(data);
      setPortfolioError(null);
      setPortfolioRequiresAuth(false);
    } catch (error) {
      console.error("Error loading holdings", error);
      if (error instanceof Error && error.message === "Authentication required") {
        setPortfolioError("Login to view your personal holdings.");
        setPortfolioRequiresAuth(true);
      } else {
        setPortfolioError("Unable to load your portfolio right now.");
        setPortfolioRequiresAuth(false);
      }
      setHoldings([]);
    } finally {
      setPortfolioLoading(false);
    }
  }, []);

  const handleDeleteHolding = useCallback(
    async (id: number) => {
      try {
        setDeletingHoldingId(id);
        await deleteHolding(id);
        await loadHoldings();
      } catch (error) {
        console.error("Error deleting holding", error);
        if (error instanceof Error && error.message === "Authentication required") {
          setPortfolioError("Login to manage your holdings.");
          setPortfolioRequiresAuth(true);
        } else {
          setPortfolioError("Unable to delete that holding right now.");
        }
      } finally {
        setDeletingHoldingId(null);
      }
    },
    [loadHoldings]
  );

  const handleEditRequest = useCallback((holding: Holding) => {
    setEditingHolding(holding);
  }, []);

  const handleEditSuccess = useCallback(async () => {
    await loadHoldings();
    setEditingHolding(null);
  }, [loadHoldings]);

  useEffect(() => {
    if (editingHolding && !holdings.some(h => h.id === editingHolding.id)) {
      setEditingHolding(null);
    }
  }, [holdings, editingHolding]);

  useEffect(() => {
    if (portfolioRequiresAuth) {
      setEditingHolding(null);
    }
  }, [portfolioRequiresAuth]);

  useEffect(() => {
    if (!watchlistHydrated) return;
    loadPrices();
    const interval = setInterval(loadPrices, 10000); // refresh every 10s
    return () => clearInterval(interval);
  }, [loadPrices, watchlistHydrated]);

  useEffect(() => {
    loadHoldings();
  }, [loadHoldings]);

  useEffect(() => {
    if (typeof window === "undefined") return;
    const stored = window.localStorage.getItem(WATCHLIST_STORAGE_KEY);
    if (stored) {
      try {
        const parsed = JSON.parse(stored);
        if (Array.isArray(parsed) && parsed.every(item => typeof item === "string")) {
          setWatchlist(parsed.length ? parsed : DEFAULT_WATCHLIST);
        }
      } catch {
        // ignore malformed data
      }
    }
    setWatchlistHydrated(true);
  }, []);

  useEffect(() => {
    if (typeof window === "undefined" || !watchlistHydrated) return;
    window.localStorage.setItem(WATCHLIST_STORAGE_KEY, JSON.stringify(watchlist));
  }, [watchlist, watchlistHydrated]);

  return (
    <div className="space-y-10">
      <div className="space-y-6">
        <div className="flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between">
          <h1 className="text-3xl font-bold">Dashboard</h1>
          <div className="flex items-center gap-3 text-xs text-gray-500">
            {formattedLastPriceUpdate && (
              <span>Last update: {formattedLastPriceUpdate}</span>
            )}
            <button
              onClick={handleManualRefresh}
              className="rounded-lg border border-gray-800 px-3 py-1.5 text-xs uppercase tracking-wide text-gray-300 hover:border-purple-500 hover:text-white disabled:opacity-50"
              disabled={priceLoading}
            >
              Refresh prices
            </button>
          </div>
        </div>

        <WatchlistControls
          watchlist={watchlist}
          onAdd={handleAddWatchlist}
          onRemove={handleRemoveWatchlist}
        />

        {priceError && (
          <div className="rounded-xl border border-red-500/30 bg-red-500/10 p-4 text-sm text-red-200">
            {priceError}
          </div>
        )}

        {hasWatchlist ? (
          <div className="grid grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-3">
            {watchlist.map(symbol => (
              <CryptoCard
                key={symbol}
                symbol={symbol}
                price={prices[symbol]}
                loading={priceLoading}
                currency="EUR"
                onRemove={() => handleRemoveWatchlist(symbol)}
              />
            ))}
          </div>
        ) : (
          <div className="rounded-2xl border border-dashed border-gray-800 bg-gray-950/40 p-6 text-sm text-gray-400">
            Add at least one symbol to start tracking live prices.
          </div>
        )}
      </div>

      <section className="space-y-8">
        <div className="flex items-center justify-between gap-4 mb-4">
          <div>
            <h2 className="text-2xl font-semibold">Your holdings</h2>
            <p className="text-sm text-gray-500">Synced from the backend portfolio service.</p>
          </div>
          <div className="flex items-center gap-3">
            <button
              onClick={loadHoldings}
              className="text-sm text-purple-300 hover:text-white transition disabled:opacity-50"
              disabled={portfolioLoading}
            >
              Refresh
            </button>
          </div>
        </div>

        <HoldingsControls
          filter={symbolFilter}
          sort={sortKey}
          onFilterChange={setSymbolFilter}
          onSortChange={setSortKey}
          disabled={portfolioRequiresAuth}
        />

        <PortfolioSummary holdings={displayHoldings} prices={prices} loadingPrices={priceLoading} />

        {holdings.length > 0 && (
          <p className="text-xs uppercase tracking-wide text-gray-500">
            Showing {displayHoldings.length} of {holdings.length} holdings
            {isFiltering ? " (filtered)" : ""}
          </p>
        )}

        {!portfolioRequiresAuth && (
          <div className="grid gap-6 lg:grid-cols-2">
            <AddHoldingForm holdings={holdings} onSuccess={loadHoldings} />
            {editingHolding ? (
              <EditHoldingForm
                holding={editingHolding}
                onCancel={() => setEditingHolding(null)}
                onSuccess={handleEditSuccess}
              />
            ) : (
              <div className="rounded-xl border border-gray-800 bg-gray-900/40 p-6 text-sm text-gray-400">
                Select a holding to edit from the table below.
              </div>
            )}
          </div>
        )}

        <PriceTable
          holdings={displayHoldings}
          prices={prices}
          loading={portfolioLoading}
          error={portfolioError}
          onDelete={portfolioRequiresAuth ? undefined : handleDeleteHolding}
          deletingId={deletingHoldingId}
          onEdit={portfolioRequiresAuth ? undefined : handleEditRequest}
          editingId={editingHolding?.id ?? null}
          emptyMessage={emptyMessage}
        />
      </section>
    </div>
  );
}
