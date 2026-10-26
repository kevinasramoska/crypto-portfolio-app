import { act, renderHook, waitFor } from "@testing-library/react";
import { useDashboardData } from "./useDashboardData";
import * as api from "@/lib/api";
import { clearAuthSession, saveToken } from "@/lib/auth";
import type { PortfolioPerformanceHistory } from "@/lib/types";

vi.mock("@/lib/api", () => ({
  AUTH_REQUIRED_MESSAGE: "Authentication required",
  getHoldings: vi.fn(), getPortfolioSummary: vi.fn(), getTransactionSummary: vi.fn(),
  getTransactions: vi.fn(), getTransactionsPaginated: vi.fn(), getPortfolioPerformanceHistory: vi.fn(),
  createTransaction: vi.fn(), updateTransaction: vi.fn(), deleteTransaction: vi.fn(),
}));

function deferred<T>() {
  let resolve!: (value: T) => void;
  const promise = new Promise<T>(done => { resolve = done; });
  return { promise, resolve };
}

describe("dashboard request lifecycle", () => {
  afterEach(() => {
    vi.restoreAllMocks();
    vi.unstubAllGlobals();
  });
  beforeEach(() => {
    vi.resetAllMocks();
    window.localStorage.clear();
    saveToken("test-token");
    vi.mocked(api.getHoldings).mockResolvedValue([]);
    vi.mocked(api.getPortfolioSummary).mockResolvedValue({ holdings: [] } as never);
    vi.mocked(api.getTransactionSummary).mockResolvedValue({} as never);
    vi.mocked(api.getTransactionsPaginated).mockResolvedValue({ content: [], totalPages: 0, totalElements: 0, hasNext: false, hasPrevious: false } as never);
    vi.mocked(api.getPortfolioPerformanceHistory).mockImplementation(async range => ({ range, history: [] }));
  });

  it("ignores an older range response delivered after the current range", async () => {
    const { result } = renderHook(useDashboardData);
    await waitFor(() => expect(result.current.performanceLoading).toBe(false));
    const older = deferred<PortfolioPerformanceHistory>();
    const newer = deferred<PortfolioPerformanceHistory>();
    vi.mocked(api.getPortfolioPerformanceHistory).mockImplementation(range => range === "7d" ? older.promise : newer.promise);
    act(() => result.current.setPerformanceRange("7d"));
    await waitFor(() => expect(api.getPortfolioPerformanceHistory).toHaveBeenCalledWith("7d"));
    act(() => result.current.setPerformanceRange("90d"));
    await waitFor(() => expect(api.getPortfolioPerformanceHistory).toHaveBeenCalledWith("90d"));
    await act(async () => newer.resolve({ range: "90d", history: [] }));
    await act(async () => older.resolve({ range: "7d", history: [] }));
    expect(result.current.performanceRange).toBe("90d");
    expect(result.current.performanceHistory?.range).toBe("90d");
  });

  it("clears protected data on logout and rejects pending authenticated responses", async () => {
    const { result } = renderHook(useDashboardData);
    await waitFor(() => expect(result.current.portfolioDataLoading).toBe(false));
    const pending = deferred<Awaited<ReturnType<typeof api.getHoldings>>>();
    vi.mocked(api.getHoldings).mockReturnValue(pending.promise);
    let refresh!: Promise<void>;
    act(() => { refresh = result.current.refreshDashboard(); });
    act(() => clearAuthSession());
    await act(async () => {
      pending.resolve([{ symbol: "PRIVATE" }] as never);
      await refresh;
    });
    expect(result.current.holdings).toEqual([]);
    expect(result.current.portfolioSummary).toBeNull();
    expect(result.current.portfolioRequiresAuth).toBe(true);
  });

  it("refreshes the current page and range when an older save finishes", async () => {
    vi.mocked(api.getTransactionsPaginated).mockImplementation(async page => ({
      content: [{ id: (page ?? 0) + 1 }], totalPages: 2, totalElements: 2,
      hasNext: page === 0, hasPrevious: page === 1,
    } as never));
    const { result } = renderHook(useDashboardData);
    await waitFor(() => expect(result.current.transactionLoading || result.current.performanceLoading).toBe(false));
    const saving = deferred<Awaited<ReturnType<typeof api.createTransaction>>>();
    vi.mocked(api.createTransaction).mockReturnValue(saving.promise);
    let mutation!: Promise<void>;
    act(() => { mutation = result.current.handleCreateTransaction({ symbol: "BTC", name: "Bitcoin", type: "BUY", quantity: 1, priceUsd: 1 }); });
    act(() => {
      result.current.setPerformanceRange("90d");
      result.current.handleTransactionPageChange(1);
    });
    await waitFor(() => expect(result.current.performanceHistory?.range).toBe("90d"));
    await waitFor(() => expect(result.current.transactions[0]?.id).toBe(2));
    await act(async () => {
      saving.resolve({ id: 3 } as never);
      await mutation;
    });
    expect(result.current.performanceRange).toBe("90d");
    expect(result.current.performanceHistory?.range).toBe("90d");
    expect(result.current.transactionPageNumber).toBe(1);
    expect(result.current.transactions[0]?.id).toBe(2);
  });

  it("exports one bounded history snapshot instead of offset pages that can shift during edits", async () => {
    const row = (id: number) => ({ id, symbol: "BTC", name: `Record-${id}`, type: "BUY", createdAt: "2026-10-26", quantity: 1, priceUsd: 1, totalValueUsd: 1, realisedProfitUsd: 0 });
    vi.mocked(api.getTransactions).mockResolvedValue([row(3), row(2), row(1)] as never);
    // Deleting a newer row between offset pages shifts record 1 out of page 1.
    vi.mocked(api.getTransactionsPaginated).mockImplementation(async page => ({
      content: page === 0 ? [row(3), row(2)] : [], totalPages: 2, totalElements: 3,
      hasNext: page === 0, hasPrevious: page === 1,
    } as never));
    const { result } = renderHook(useDashboardData);
    await waitFor(() => expect(result.current.transactionLoading).toBe(false));
    const OriginalBlob = globalThis.Blob;
    let csv = "";
    vi.spyOn(globalThis, "Blob").mockImplementation(function (parts, options) {
      csv = String(parts?.[0]);
      return new OriginalBlob(parts, options);
    });
    vi.stubGlobal("URL", { createObjectURL: vi.fn(() => "blob:export"), revokeObjectURL: vi.fn() });
    vi.spyOn(HTMLAnchorElement.prototype, "click").mockImplementation(() => {});
    await act(async () => result.current.handleExportTransactions());
    expect(csv).toContain("Record-1");
    expect(api.getTransactions).toHaveBeenCalledTimes(1);
  });

  it("returns to the previous page after deleting its last row without reloading the deleted page", async () => {
    vi.mocked(api.getTransactionsPaginated).mockImplementation(async page => ({
      content: page === 1 ? [{ id: 21, symbol: "BTC" }] : [],
      totalPages: 2, totalElements: 21, hasNext: page === 0, hasPrevious: page === 1,
    } as never));
    const { result } = renderHook(useDashboardData);
    await waitFor(() => expect(result.current.transactionLoading).toBe(false));
    act(() => result.current.handleTransactionPageChange(1));
    await waitFor(() => expect(result.current.transactions).toHaveLength(1));
    vi.mocked(api.getTransactionsPaginated).mockClear();
    await act(async () => result.current.handleDeleteTransaction(21));
    await waitFor(() => expect(api.getTransactionsPaginated).toHaveBeenCalledWith(0, 20));
    expect(api.getTransactionsPaginated).not.toHaveBeenCalledWith(1, 20);
    expect(result.current.transactionPageNumber).toBe(0);
  });

  it("does not move the newly selected page when an older delete finishes", async () => {
    vi.mocked(api.getTransactionsPaginated).mockImplementation(async page => ({
      content: [{ id: (page ?? 0) + 1 }], totalPages: 3, totalElements: 3,
      hasNext: page !== 2, hasPrevious: page !== 0,
    } as never));
    const { result } = renderHook(useDashboardData);
    await waitFor(() => expect(result.current.transactionLoading).toBe(false));
    act(() => result.current.handleTransactionPageChange(1));
    await waitFor(() => expect(result.current.transactions[0]?.id).toBe(2));
    const deleting = deferred<void>();
    vi.mocked(api.deleteTransaction).mockReturnValue(deleting.promise);
    let mutation!: Promise<void>;
    act(() => { mutation = result.current.handleDeleteTransaction(2); });
    act(() => result.current.handleTransactionPageChange(2));
    await waitFor(() => expect(result.current.transactions[0]?.id).toBe(3));
    await act(async () => { deleting.resolve(); await mutation; });
    expect(result.current.transactionPageNumber).toBe(2);
    expect(result.current.transactions[0]?.id).toBe(3);
  });

  it("rejects an oversized export before creating a download", async () => {
    vi.mocked(api.getTransactions).mockResolvedValue(Array(10001).fill({}) as never);
    vi.spyOn(console, "error").mockImplementation(() => {});
    const blob = vi.spyOn(globalThis, "Blob");
    const { result } = renderHook(useDashboardData);
    await waitFor(() => expect(result.current.transactionLoading).toBe(false));
    await act(async () => result.current.handleExportTransactions());
    expect(result.current.transactionError).toBe("Export is limited to 10,000 transactions.");
    expect(blob).not.toHaveBeenCalled();
    expect(result.current.exportTransactionsLoading).toBe(false);
  });

  it("does not download an export after its session ends", async () => {
    const pending = deferred<Awaited<ReturnType<typeof api.getTransactions>>>();
    vi.mocked(api.getTransactions).mockReturnValue(pending.promise);
    vi.spyOn(console, "error").mockImplementation(() => {});
    const blob = vi.spyOn(globalThis, "Blob");
    const { result } = renderHook(useDashboardData);
    await waitFor(() => expect(result.current.transactionLoading).toBe(false));
    let exporting!: Promise<void>;
    act(() => { exporting = result.current.handleExportTransactions(); });
    act(() => clearAuthSession());
    await act(async () => { pending.resolve([]); await exporting; });
    expect(result.current.transactionError).toBe("Login to export your transactions.");
    expect(blob).not.toHaveBeenCalled();
    expect(result.current.exportTransactionsLoading).toBe(false);
  });
});
