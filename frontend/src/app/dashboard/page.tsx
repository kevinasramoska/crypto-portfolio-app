"use client";

import { useCallback, useEffect, useMemo, useState } from "react";
import AddTransactionForm from "@/components/AddTransactionForm";
import AssetAllocationSummary from "@/components/AssetAllocationSummary";
import CryptoCard from "@/components/CryptoCard";
import HoldingsControls, { type SortKey } from "@/components/HoldingsControls";
import PerformanceHistory from "@/components/PerformanceHistory";
import PortfolioSummaryCards from "@/components/PortfolioSummaryCards";
import PriceTable from "@/components/PriceTable";
import TransactionSummaryCards from "@/components/TransactionSummaryCards";
import TransactionsTable from "@/components/TransactionsTable";
import WatchlistControls from "@/components/WatchlistControls";
import Link from "next/link";
import { getPrices } from "@/lib/api";
import { useDashboardData } from "@/hooks/useDashboardData";
import { PriceMap } from "@/lib/types";

const DEFAULT_WATCHLIST = ["BTC", "ETH", "SOL", "LINK", "DOGE"];
const WATCHLIST_STORAGE_KEY = "crypto-dashboard-watchlist";

function formatTimestamp(date: Date | null): { absolute: string; relative: string } | null {
  if (!date) return null;

  const now = new Date();
  const diffMs = now.getTime() - date.getTime();
  const diffMins = Math.floor(diffMs / 60000);
  const diffHours = Math.floor(diffMs / 3600000);
  const diffDays = Math.floor(diffMs / 86400000);

  let relative: string;
  if (diffMins < 1) {
    relative = "just now";
  } else if (diffMins < 60) {
    relative = `${diffMins}m ago`;
  } else if (diffHours < 24) {
    relative = `${diffHours}h ago`;
  } else {
    relative = `${diffDays}d ago`;
  }

  const absolute = date.toLocaleString(undefined, {
    month: "short",
    day: "numeric",
    hour: "2-digit",
    minute: "2-digit",
    second: "2-digit",
  });

  return { absolute, relative };
}

export default function DashboardPage() {
  const [prices, setPrices] = useState<PriceMap>({});
  const [priceLoading, setPriceLoading] = useState(true);
  const [priceError, setPriceError] = useState<string | null>(null);
  const [lastPriceUpdated, setLastPriceUpdated] = useState<Date | null>(null);
  const [watchlist, setWatchlist] = useState<string[]>(DEFAULT_WATCHLIST);
  const [watchlistHydrated, setWatchlistHydrated] = useState(false);
  const [symbolFilter, setSymbolFilter] = useState("");
  const [sortKey, setSortKey] = useState<SortKey>("value");
  const {
    holdings,
    portfolioSummary,
    lastPortfolioUpdated,
    transactions,
    transactionSummary,
    performanceHistory,
    performanceRange,
    setPerformanceRange,
    portfolioDataLoading,
    portfolioDataError,
    portfolioRequiresAuth,
    transactionLoading,
    transactionError,
    exportTransactionsLoading,
    performanceLoading,
    performanceError,
    transactionPageNumber,
    transactionPageSize,
    transactionTotalElements,
    transactionTotalPages,
    transactionHasNext,
    transactionHasPrevious,
    refreshDashboard,
    handleCreateTransaction,
    handleUpdateTransaction,
    handleDeleteTransaction,
    handleTransactionPageChange,
    handleExportTransactions,
  } = useDashboardData();

  const displayHoldings = useMemo(() => {
    const normalizedFilter = symbolFilter.trim().toLowerCase();
    let filtered = holdings;

    if (normalizedFilter) {
      filtered = holdings.filter(
        holding =>
          holding.symbol.toLowerCase().includes(normalizedFilter) ||
          holding.name.toLowerCase().includes(normalizedFilter)
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
      return b.currentValueUsd - a.currentValueUsd;
    });

    return sorted;
  }, [holdings, symbolFilter, sortKey]);

  const hasWatchlist = watchlist.length > 0;
  const priceTimestamp = formatTimestamp(lastPriceUpdated);
  const portfolioTimestamp = formatTimestamp(lastPortfolioUpdated);
  const isFiltering = symbolFilter.trim().length > 0;
  const dashboardLoading = portfolioDataLoading || transactionLoading || performanceLoading;
  const emptyMessage =
    holdings.length === 0
      ? "No open holdings yet. Record a buy transaction to create your first position."
      : "No holdings match your current filters.";
  const isNewPortfolio = !portfolioDataLoading && holdings.length === 0;

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

  useEffect(() => {
    if (!watchlistHydrated) return;
    loadPrices();
    const interval = setInterval(loadPrices, 10000);
    return () => clearInterval(interval);
  }, [loadPrices, watchlistHydrated]);

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
        // ignore malformed storage
      }
    }
    setWatchlistHydrated(true);
  }, []);

  useEffect(() => {
    if (typeof window === "undefined" || !watchlistHydrated) return;
    window.localStorage.setItem(WATCHLIST_STORAGE_KEY, JSON.stringify(watchlist));
  }, [watchlist, watchlistHydrated]);

  return (
    <div className="space-y-12">
       <section className="space-y-6">
         <div className="flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between">
           <div>
             <h1 className="text-3xl font-bold">Dashboard</h1>
             <p className="mt-1 text-sm text-gray-500">Explore live markets or sign in to manage your portfolio.</p>
           </div>
           <div className="flex items-center gap-3 text-xs text-gray-500">
             {priceTimestamp && (
               <div className="flex flex-col items-end gap-0.5">
                 <span>Prices {priceTimestamp.relative}</span>
                 <span className="text-xs text-gray-600">{priceTimestamp.absolute}</span>
               </div>
             )}
             <button
               onClick={loadPrices}
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
                currency="USD"
                onRemove={() => handleRemoveWatchlist(symbol)}
              />
            ))}
          </div>
        ) : (
          <div className="rounded-xl border border-dashed border-gray-800 bg-gray-950/40 p-6 text-sm text-gray-400">
            Add at least one symbol to start tracking live prices.
          </div>
        )}
      </section>

      <section className="space-y-6">
         <div className="flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between">
           <div>
             <h2 className="text-2xl font-semibold">Portfolio summary</h2>
             <p className="text-sm text-gray-500">Totals, holdings, realised P/L, and unrealised P/L from the backend.</p>
           </div>
           <div className="flex flex-col items-start gap-2 sm:items-end">
             {portfolioTimestamp && (
               <div className="flex flex-col items-end gap-0.5">
                 <span className="text-xs text-gray-500">Portfolio {portfolioTimestamp.relative}</span>
                 <span className="text-xs text-gray-600">{portfolioTimestamp.absolute}</span>
               </div>
             )}
             <button
               onClick={refreshDashboard}
               className="text-sm text-purple-300 transition hover:text-white disabled:opacity-50"
               disabled={dashboardLoading}
             >
               Refresh portfolio
             </button>
           </div>
         </div>

        {portfolioRequiresAuth ? (
          <div className="rounded-2xl border border-purple-500/30 bg-gradient-to-r from-purple-500/15 to-purple-950/20 p-6 sm:p-8">
            <p className="text-xs font-semibold uppercase tracking-[0.18em] text-purple-300">Your portfolio starts here</p>
            <h2 className="mt-2 text-2xl font-semibold text-white">Create an account to track your own holdings.</h2>
            <p className="mt-2 max-w-2xl text-sm leading-6 text-gray-300">
              The market watchlist is open to explore. Register to record transactions, calculate profit and loss, and
              see your portfolio history.
            </p>
            <div className="mt-5 flex flex-col gap-3 sm:flex-row">
              <Link href="/register" className="rounded-lg bg-purple-600 px-5 py-3 text-center text-sm font-semibold text-white transition hover:bg-purple-500">
                Create account
              </Link>
              <Link href="/login" className="rounded-lg border border-purple-400/50 px-5 py-3 text-center text-sm font-semibold text-purple-100 transition hover:bg-purple-500/10">
                Log in
              </Link>
            </div>
          </div>
        ) : portfolioDataError ? (
          <div className="rounded-xl border border-red-500/40 bg-red-500/10 p-6 text-sm text-red-200">
            {portfolioDataError}
          </div>
        ) : (
          <PortfolioSummaryCards summary={portfolioSummary} loading={portfolioDataLoading} />
        )}
      </section>

      {!portfolioRequiresAuth && (
        <>
          {isNewPortfolio && (
            <section className="rounded-2xl border border-purple-500/25 bg-purple-500/5 p-6 sm:p-8">
              <p className="text-xs font-semibold uppercase tracking-[0.18em] text-purple-300">Getting started</p>
              <h2 className="mt-2 text-2xl font-semibold text-white">Build your portfolio in three steps.</h2>
              <ol className="mt-5 grid gap-4 text-sm text-gray-300 sm:grid-cols-3">
                <li className="rounded-xl border border-gray-800 bg-gray-950/50 p-4"><span className="font-semibold text-purple-200">1. Record a buy</span><p className="mt-1 text-gray-400">Add your coin, quantity, and purchase price.</p></li>
                <li className="rounded-xl border border-gray-800 bg-gray-950/50 p-4"><span className="font-semibold text-purple-200">2. Review holdings</span><p className="mt-1 text-gray-400">See current value, cost basis, and open P/L.</p></li>
                <li className="rounded-xl border border-gray-800 bg-gray-950/50 p-4"><span className="font-semibold text-purple-200">3. Build history</span><p className="mt-1 text-gray-400">Performance appears as snapshots are recorded over time.</p></li>
              </ol>
              <a href="#record-transaction" className="mt-5 inline-flex rounded-lg bg-purple-600 px-5 py-3 text-sm font-semibold text-white transition hover:bg-purple-500">
                Record your first buy
              </a>
            </section>
          )}

          <section className="space-y-6">
            <div>
              <h2 className="text-2xl font-semibold">Asset allocation</h2>
              <p className="text-sm text-gray-500">Share of known portfolio value by open holding.</p>
            </div>
            <AssetAllocationSummary holdings={holdings} loading={portfolioDataLoading} error={portfolioDataError} />
          </section>

          <section className="grid gap-6 lg:grid-cols-[minmax(0,1fr)_minmax(320px,420px)]">
            <div className="space-y-6">
              <div>
                <h2 className="text-2xl font-semibold">Holdings</h2>
                <p className="text-sm text-gray-500">Open positions calculated by the backend portfolio service.</p>
              </div>

              <HoldingsControls
                filter={symbolFilter}
                sort={sortKey}
                onFilterChange={setSymbolFilter}
                onSortChange={setSortKey}
              />

              {holdings.length > 0 && (
                <p className="text-xs uppercase tracking-wide text-gray-500">
                  Showing {displayHoldings.length} of {holdings.length} holdings
                  {isFiltering ? " (filtered)" : ""}
                </p>
              )}

              <PriceTable
                holdings={displayHoldings}
                loading={portfolioDataLoading}
                error={portfolioDataError}
                emptyMessage={emptyMessage}
              />
            </div>

            <div id="record-transaction" className={isNewPortfolio ? "order-first" : undefined}>
              <AddTransactionForm
                onSubmit={payload => handleCreateTransaction(payload, loadPrices)}
                disabled={portfolioDataLoading || transactionLoading}
              />
            </div>
          </section>

          <section className="space-y-6">
            <div>
              <h2 className="text-2xl font-semibold">Transaction summary</h2>
              <p className="text-sm text-gray-500">Buy, sell, and realised profit totals.</p>
            </div>
            {transactionError ? (
              <div className="rounded-xl border border-red-500/40 bg-red-500/10 p-6 text-sm text-red-200">
                {transactionError}
              </div>
            ) : (
              <TransactionSummaryCards summary={transactionSummary} loading={transactionLoading} />
            )}
          </section>

          <PerformanceHistory
            history={performanceHistory}
            range={performanceRange}
            loading={performanceLoading}
            error={performanceError}
            onRangeChange={setPerformanceRange}
          />

          <section className="space-y-6">
            <div>
              <h2 className="text-2xl font-semibold">Transactions</h2>
              <p className="text-sm text-gray-500">Most recent records from the backend transaction service.</p>
            </div>
            <TransactionsTable
              transactions={transactions}
              loading={transactionLoading}
              error={transactionError}
              exportLoading={exportTransactionsLoading}
              onExportCsv={handleExportTransactions}
              pageNumber={transactionPageNumber}
              pageSize={transactionPageSize}
              totalElements={transactionTotalElements}
              totalPages={transactionTotalPages}
              hasNext={transactionHasNext}
              hasPrevious={transactionHasPrevious}
              onPageChange={handleTransactionPageChange}
              onEdit={handleUpdateTransaction}
              onDelete={handleDeleteTransaction}
            />
          </section>
        </>
      )}
    </div>
  );
}
