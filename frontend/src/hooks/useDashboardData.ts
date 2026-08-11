"use client";

import { useCallback, useEffect, useState } from "react";
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
import { getToken } from "@/lib/auth";
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

function escapeCsvValue(value: string | number) {
  const normalized = String(value).replaceAll('"', '""');
  return `"${normalized}"`;
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
  const [holdings, setHoldings] = useState<Holding[]>([]);
  const [portfolioSummary, setPortfolioSummary] = useState<PortfolioSummary | null>(null);
  const [lastPortfolioUpdated, setLastPortfolioUpdated] = useState<Date | null>(null);
  const [transactions, setTransactions] = useState<Transaction[]>([]);
  const [transactionSummary, setTransactionSummary] = useState<TransactionSummary | null>(null);
  const [performanceHistory, setPerformanceHistory] = useState<PortfolioPerformanceHistory | null>(null);
  const [performanceRange, setPerformanceRange] = useState<PerformanceRange>("30d");

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

      setHoldings(holdingsData);
      setPortfolioSummary(summaryData);
      setLastPortfolioUpdated(new Date());
      setPortfolioRequiresAuth(false);
    } catch (error) {
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
      setPortfolioDataLoading(false);
    }
  }, []);

  const loadTransactionData = useCallback(async () => {
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
        getTransactionsPaginated(transactionPageNumber, transactionPageSize),
        getTransactionSummary(),
      ]);

      setTransactions(paginatedData.content);
      setTransactionTotalElements(paginatedData.totalElements);
      setTransactionTotalPages(paginatedData.totalPages);
      setTransactionHasNext(paginatedData.hasNext);
      setTransactionHasPrevious(paginatedData.hasPrevious);
      setTransactionSummary(transactionSummaryData);
    } catch (error) {
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
      setTransactionLoading(false);
    }
  }, [transactionPageNumber, transactionPageSize]);

  const loadPerformanceData = useCallback(async () => {
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

      const performanceData = await getPortfolioPerformanceHistory(performanceRange);
      setPerformanceHistory(performanceData);
    } catch (error) {
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
      setPerformanceLoading(false);
    }
  }, [performanceRange]);

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
      if (transactions.length === 1 && transactionPageNumber > 0) {
        setTransactionPageNumber(previousPage => previousPage - 1);
      }
      await refreshDashboard();
    },
    [refreshDashboard, transactionPageNumber, transactions.length]
  );

  const handleExportTransactions = useCallback(async () => {
    try {
      setExportTransactionsLoading(true);

      const allTransactions = await getTransactions();
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
        setTransactionError("Unable to export transactions right now.");
      }
    } finally {
      setExportTransactionsLoading(false);
    }
  }, []);

  useEffect(() => {
    loadPortfolioData();
    loadTransactionData();
  }, [loadPortfolioData, loadTransactionData]);

  useEffect(() => {
    loadPerformanceData();
  }, [loadPerformanceData]);

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
