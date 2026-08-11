import { authHeader, clearAuthSession } from "./auth";
import {
  Holding,
  LoginResponse,
  PaginatedTransactions,
  PerformanceRange,
  PortfolioPerformanceHistory,
  PortfolioSummary,
  PriceMap,
  SupportedCoin,
  Transaction,
  TransactionPayload,
  TransactionSummary,
} from "./types";

const API_BASE = process.env.NEXT_PUBLIC_API_BASE_URL ?? "http://localhost:8080/api";
export const AUTH_REQUIRED_MESSAGE = "Authentication required";

async function parseJson<T>(res: Response, errorMessage: string): Promise<T> {
  if (!res.ok) {
    throw new Error(errorMessage);
  }
  return res.json();
}

async function parseOptionalJson<T>(res: Response, errorMessage: string): Promise<T> {
  if (!res.ok) {
    let detail = errorMessage;
    try {
      const body = await res.json();
      if (typeof body?.error === "string") {
        detail = body.error;
      }
    } catch {
      // keep fallback message
    }
    throw new Error(detail);
  }
  return res.json();
}

async function authorizedFetch(path: string, init?: RequestInit) {
  const headers = new Headers(init?.headers);
  headers.set("Content-Type", "application/json");

  const authorization = authHeader().Authorization;
  if (authorization) {
    headers.set("Authorization", authorization);
  }

  const res = await fetch(`${API_BASE}${path}`, {
    ...init,
    headers,
  });

  if (res.status === 401 || res.status === 403) {
    clearAuthSession();
    throw new Error(AUTH_REQUIRED_MESSAGE);
  }

  return res;
}

function toNumber(value: number | string | null | undefined) {
  const parsed = typeof value === "string" ? Number(value) : value;
  return typeof parsed === "number" && Number.isFinite(parsed) ? parsed : 0;
}

export async function getPrices(symbols: string[]): Promise<PriceMap> {
  const res = await fetch(`${API_BASE}/market/prices?symbols=${symbols.join(",")}`);
  return parseJson(res, "Failed to fetch prices");
}

export async function getSupportedCoins(): Promise<SupportedCoin[]> {
  const res = await fetch(`${API_BASE}/market/supported-coins`);
  return parseJson(res, "Failed to fetch supported coins");
}

export async function login(email: string, password: string): Promise<LoginResponse> {
  const res = await fetch(`${API_BASE}/auth/login`, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ email, password }),
  });

  return parseJson(res, "Login failed");
}

export async function register(email: string, password: string): Promise<LoginResponse> {
  const res = await fetch(`${API_BASE}/auth/register`, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ email, password }),
  });

  return parseJson(res, "Registration failed");
}

type HoldingApiResponse = Omit<Holding, "quantity"> & { quantity: number | string };

export async function getHoldings(): Promise<Holding[]> {
  const res = await authorizedFetch("/portfolio/holdings");

  const data = await parseJson<HoldingApiResponse[]>(res, "Failed to fetch holdings");

  return data.map(holding => {
    return {
      ...holding,
      quantity: toNumber(holding.quantity),
      averageBuyPriceUsd: toNumber(holding.averageBuyPriceUsd),
      currentPriceUsd: toNumber(holding.currentPriceUsd),
      investedValueUsd: toNumber(holding.investedValueUsd),
      currentValueUsd: toNumber(holding.currentValueUsd),
      profitLossUsd: toNumber(holding.profitLossUsd),
      marketPriceAvailable: Boolean(holding.marketPriceAvailable),
    };
  });
}

export async function getPortfolioSummary(): Promise<PortfolioSummary> {
  const res = await authorizedFetch("/portfolio/summary");
  return parseJson(res, "Failed to fetch portfolio summary");
}

export async function getPortfolioPerformanceHistory(
  range: PerformanceRange
): Promise<PortfolioPerformanceHistory> {
  const res = await authorizedFetch(`/portfolio/performance/history?range=${range}`);
  return parseJson(res, "Failed to fetch portfolio performance history");
}

export async function getTransactions(): Promise<Transaction[]> {
  const res = await authorizedFetch("/transactions");
  return parseJson(res, "Failed to fetch transactions");
}

export async function getTransactionsPaginated(page: number = 0, size: number = 20): Promise<PaginatedTransactions> {
  const res = await authorizedFetch(`/transactions/paginated?page=${page}&size=${size}`);
  return parseJson(res, "Failed to fetch paginated transactions");
}

export async function getTransactionSummary(): Promise<TransactionSummary> {
  const res = await authorizedFetch("/transactions/summary");
  return parseJson(res, "Failed to fetch transaction summary");
}

export async function createTransaction(payload: TransactionPayload): Promise<Transaction> {
  const res = await authorizedFetch("/transactions", {
    method: "POST",
    body: JSON.stringify(payload),
  });

  return parseOptionalJson(res, "Failed to create transaction");
}

export async function updateTransaction(id: number, payload: TransactionPayload): Promise<Transaction> {
  const res = await authorizedFetch(`/transactions/${id}`, {
    method: "PUT",
    body: JSON.stringify(payload),
  });

  return parseOptionalJson(res, "Failed to update transaction");
}

export async function deleteTransaction(id: number): Promise<void> {
  const res = await authorizedFetch(`/transactions/${id}`, {
    method: "DELETE",
  });

  if (!res.ok) {
    await parseOptionalJson(res, "Failed to delete transaction");
  }
}
