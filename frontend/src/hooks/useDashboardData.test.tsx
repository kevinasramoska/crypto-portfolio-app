import { act, renderHook, waitFor } from "@testing-library/react";
import { useDashboardData } from "./useDashboardData";
import * as api from "@/lib/api";
import { clearAuthSession, saveToken } from "@/lib/auth";
import type { PortfolioPerformanceHistory } from "@/lib/types";

vi.mock("@/lib/api", () => ({
  AUTH_REQUIRED_MESSAGE: "Authentication required",
  getHoldings: vi.fn(), getPortfolioSummary: vi.fn(), getTransactionSummary: vi.fn(),
  getTransactionsPaginated: vi.fn(), getPortfolioPerformanceHistory: vi.fn(),
  createTransaction: vi.fn(), updateTransaction: vi.fn(), deleteTransaction: vi.fn(),
}));

function deferred<T>() {
  let resolve!: (value: T) => void;
  const promise = new Promise<T>(done => { resolve = done; });
  return { promise, resolve };
}

describe("dashboard request lifecycle", () => {
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
});
