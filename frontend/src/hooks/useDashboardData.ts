"use client";

import { useCallback, useEffect, useRef, useState } from "react";
import {
  AUTH_REQUIRED_MESSAGE,
  createTransaction,
  deleteTransaction,
  getHoldings,
  getPortfolioPerformanceHistory,
  getPortfolioSummary,
  getTransactionSummary,
  getTransactions,
  getTransactionsPaginated,
  updateTransaction,
} from "@/lib/api";
import { getToken, subscribeAuth } from "@/lib/auth";
import { escapeCsvValue } from "@/lib/csv";
import {
  Holding,
  PerformanceRange,
  PortfolioPerformanceHistory,
  PortfolioSummary,
  Transaction,
  TransactionPayload,
  TransactionSummary,
} from "@/lib/types";

function isAuthError(error: unknown) {
  return error instanceof Error && error.message === AUTH_REQUIRED_MESSAGE;
}

function buildTransactionsCsv(transactions: Transaction[]) {
  const headers = [
    "Date",
    "Type",
    "Symbol",
    "Name",
    "Quantity",
    "Price USD",
    "Total USD",
    "Realised P/L USD",
  ];

  const rows = transactions.map(transaction => [
    transaction.createdAt,
    transaction.type,
    transaction.symbol,
    transaction.name,
    transaction.quantity,
    transaction.priceUsd,
    transaction.totalValueUsd,
    transaction.realisedProfitUsd,
  ]);

  return [headers, ...rows]
    .map(row => row.map(escapeCsvValue).join(","))
    .join("\n");
}

export function useDashboardData() {
  const requests = useRef({ portfolio: 0, transactions: 0, performance: 0 });
  // Pending mutations must refresh the selection at completion, not at submission.
  const selection = useRef<{ range: PerformanceRange; page: number }>({ range: "30d", page: 0 });
  const [holdings, setHoldings] = useState<Holding[]>([]);
  const [portfolioSummary, setPortfolioSummary] = useState<PortfolioSummary | null>(null);
  const [lastPortfolioUpdated, setLastPortfolioUpdated] = useState<Date | null>(null);
  const [transactions, setTransactions] = useState<Transaction[]>([]);
  const [transactionSummary, setTransactionSummary] = useState<TransactionSummary | null>(null);
  const [performanceHistory, setPerformanceHistory] = useState<PortfolioPerformanceHistory | null>(null);
  const [performanceRange, updatePerformanceRange] = useState<PerformanceRange>("30d");
  const setPerformanceRange = useCallback((range: PerformanceRange) => {
    if (range === selection.current.range) return;
    selection.current.range = range;
    requests.current.performance++;
    updatePerformanceRange(range);
  }, []);

  const [portfolioDataLoading, setPortfolioDataLoading] = useState(true);
  const [portfolioDataError, setPortfolioDataError] = useState<string | null>(null);
  const [portfolioRequiresAuth, setPortfolioRequiresAuth] = useState(false);
  const [transactionLoading, setTransactionLoading] = useState(true);
  const [transactionError, setTransactionError] = useState<string | null>(null);
  const [exportTransactionsLoading, setExportTransactionsLoading] = useState(false);
  const [performanceLoading, setPerformanceLoading] = useState(true);
  const [performanceError, setPerformanceError] = useState<string | null>(null);

  const [transactionPageNumber, setTransactionPageNumber] = useState(0);
  const [transactionPageSize] = useState(20);
  const [transactionTotalElements, setTransactionTotalElements] = useState(0);
  const [transactionTotalPages, setTransactionTotalPages] = useState(0);
  const [transactionHasNext, setTransactionHasNext] = useState(false);
  const [transactionHasPrevious, setTransactionHasPrevious] = useState(false);

  const loadPortfolioData = useCallback(async () => {
    const generation = ++requests.current.portfolio;
    const token = getToken();
    const current = () => generation === requests.current.portfolio && token === getToken();
    if (!getToken()) {
      setPortfolioDataError("Login to view your portfolio.");
      setPortfolioRequiresAuth(true);
      setHoldings([]);
      setPortfolioSummary(null);
      setLastPortfolioUpdated(null);
      setPortfolioDataLoading(false);
      return;
    }

    try {
      setPortfolioDataLoading(true);
      setPortfolioDataError(null);

      const [holdingsData, summaryData] = await Promise.all([
        getHoldings(),
        getPortfolioSummary(),
      ]);
      if (!current()) return;

      setHoldings(holdingsData);
      setPortfolioSummary(summaryData);
      setLastPortfolioUpdated(summaryData.hasStaleMarketData || summaryData.hasUnsupportedMarketData ? null : new Date());
      setPortfolioRequiresAuth(false);
    } catch (error) {
      if (!current()) return;
      if (isAuthError(error)) {
        setPortfolioDataError("Login to view your portfolio.");
        setPortfolioRequiresAuth(true);
      } else {
        console.error("Error loading portfolio data", error);
        setPortfolioDataError("Unable to load your portfolio right now.");
      }
      setHoldings([]);
      setPortfolioSummary(null);
      setLastPortfolioUpdated(null);
    } finally {
      if (current()) setPortfolioDataLoading(false);
    }
  }, []);

  const loadTransactionData = useCallback(async () => {
    const generation = ++requests.current.transactions;
    const token = getToken();
    const current = () => generation === requests.current.transactions && token === getToken();
    if (!getToken()) {
      setTransactionError("Login to view your transactions.");
      setPortfolioDataError("Login to view your portfolio.");
      setPortfolioRequiresAuth(true);
      setTransactions([]);
      setTransactionSummary(null);
      setTransactionLoading(false);
      return;
    }

    try {
      setTransactionLoading(true);
      setTransactionError(null);

      const [paginatedData, transactionSummaryData] = await Promise.all([
        getTransactionsPaginated(selection.current.page, transactionPageSize),
        getTransactionSummary(),
      ]);
      if (!current()) return;

      setTransactions(paginatedData.content);
      setTransactionTotalElements(paginatedData.totalElements);
      setTransactionTotalPages(paginatedData.totalPages);
      setTransactionHasNext(paginatedData.hasNext);
      setTransactionHasPrevious(paginatedData.hasPrevious);
      setTransactionSummary(transactionSummaryData);
    } catch (error) {
      if (!current()) return;
      if (isAuthError(error)) {
        setTransactionError("Login to view your transactions.");
        setPortfolioDataError("Login to view your portfolio.");
        setPortfolioRequiresAuth(true);
      } else {
        console.error("Error loading transaction data", error);
        setTransactionError("Unable to load transactions right now.");
      }
      setTransactions([]);
      setTransactionSummary(null);
    } finally {
      if (current()) setTransactionLoading(false);
    }
  }, [transactionPageSize]);

  const loadPerformanceData = useCallback(async () => {
    const generation = ++requests.current.performance;
    const token = getToken();
    const current = () => generation === requests.current.performance && token === getToken();
    if (!getToken()) {
      setPerformanceError("Login to view performance history.");
      setPortfolioDataError("Login to view your portfolio.");
      setPortfolioRequiresAuth(true);
      setPerformanceHistory(null);
      setPerformanceLoading(false);
      return;
    }

    try {
      setPerformanceLoading(true);
      setPerformanceError(null);

      const performanceData = await getPortfolioPerformanceHistory(selection.current.range);
      if (!current()) return;
      setPerformanceHistory(performanceData);
    } catch (error) {
      if (!current()) return;
      if (isAuthError(error)) {
        setPerformanceError("Login to view performance history.");
        setPortfolioDataError("Login to view your portfolio.");
        setPortfolioRequiresAuth(true);
      } else {
        console.error("Error loading performance history", error);
        setPerformanceError("Unable to load performance history right now.");
      }
      setPerformanceHistory(null);
    } finally {
      if (current()) setPerformanceLoading(false);
    }
  }, []);

  const refreshDashboard = useCallback(async () => {
    await Promise.all([
      loadPortfolioData(),
      loadTransactionData(),
      loadPerformanceData(),
    ]);
  }, [loadPerformanceData, loadPortfolioData, loadTransactionData]);

  const handleCreateTransaction = useCallback(
    async (payload: TransactionPayload, afterSave?: () => Promise<void>) => {
      await createTransaction(payload);
      await Promise.all([
        refreshDashboard(),
        afterSave ? afterSave() : Promise.resolve(),
      ]);
    },
    [refreshDashboard]
  );

  const handleTransactionPageChange = useCallback((newPage: number) => {
    if (newPage === selection.current.page) return;
    selection.current.page = newPage;
    requests.current.transactions++;
    setTransactionPageNumber(newPage);
  }, []);

  const handleUpdateTransaction = useCallback(
    async (transactionId: number, payload: TransactionPayload) => {
      await updateTransaction(transactionId, payload);
      await refreshDashboard();
    },
    [refreshDashboard]
  );

  const handleDeleteTransaction = useCallback(
    async (transactionId: number) => {
      await deleteTransaction(transactionId);
      if (transactions.length === 1 && transactionPageNumber > 0
          && selection.current.page === transactionPageNumber) {
        const previousPage = transactionPageNumber - 1;
        selection.current.page = previousPage;
        requests.current.transactions++;
        setTransactionPageNumber(previousPage);
        await Promise.all([loadPortfolioData(), loadPerformanceData()]);
        return;
      }
      await refreshDashboard();
    },
    [refreshDashboard, transactionPageNumber, transactions.length, loadPortfolioData, loadPerformanceData]
  );

  const handleExportTransactions = useCallback(async () => {
    try {
      setExportTransactionsLoading(true);

      const token = getToken();
      const allTransactions = await getTransactions();
      if (token !== getToken()) throw new Error(AUTH_REQUIRED_MESSAGE);
      if (allTransactions.length > 10000) throw new Error("Export is limited to 10,000 transactions.");
      const csv = buildTransactionsCsv(allTransactions);
      const blob = new Blob([csv], { type: "text/csv;charset=utf-8;" });
      const url = window.URL.createObjectURL(blob);
      const link = document.createElement("a");
      const timestamp = new Date().toISOString().slice(0, 10);

      link.href = url;
      link.download = `transactions-${timestamp}.csv`;
      document.body.appendChild(link);
      link.click();
      link.remove();
      window.URL.revokeObjectURL(url);
    } catch (error) {
      console.error("Error exporting transactions", error);
      if (isAuthError(error)) {
        setTransactionError("Login to export your transactions.");
        setPortfolioDataError("Login to view your portfolio.");
        setPortfolioRequiresAuth(true);
      } else {
        setTransactionError(error instanceof Error ? error.message : "Unable to export transactions right now.");
      }
    } finally {
      setExportTransactionsLoading(false);
    }
  }, []);

  useEffect(() => subscribeAuth(() => {
    requests.current.portfolio++;
    requests.current.transactions++;
    requests.current.performance++;
    setHoldings([]);
    setPortfolioSummary(null);
    setLastPortfolioUpdated(null);
    setTransactions([]);
    setTransactionSummary(null);
    setPerformanceHistory(null);
    void refreshDashboard();
  }), [refreshDashboard]);

  useEffect(() => () => {
    requests.current.portfolio++;
    requests.current.transactions++;
    requests.current.performance++;
  }, []);

  useEffect(() => {
    const initialLoad = window.setTimeout(() => {
      void loadPortfolioData();
      void loadTransactionData();
    }, 0);
    return () => window.clearTimeout(initialLoad);
  }, [loadPortfolioData, loadTransactionData, transactionPageNumber]);

  useEffect(() => {
    const initialLoad = window.setTimeout(() => {
      void loadPerformanceData();
    }, 0);
    return () => window.clearTimeout(initialLoad);
  }, [loadPerformanceData, performanceRange]);

  return {
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
  };
}
