import { authHeader } from "./auth";
import { Holding, LoginResponse, PriceMap } from "./types";

const API_BASE = "http://localhost:8080/api";

async function parseJson<T>(res: Response, errorMessage: string): Promise<T> {
  if (!res.ok) {
    throw new Error(errorMessage);
  }
  return res.json();
}

export async function getPrices(symbols: string[]): Promise<PriceMap> {
  const res = await fetch(`${API_BASE}/market/prices?symbols=${symbols.join(",")}`);
  return parseJson(res, "Failed to fetch prices");
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
  const res = await fetch(`${API_BASE}/portfolio/holdings`, {
    headers: {
      ...authHeader(),
      "Content-Type": "application/json",
    },
  });

  if (res.status === 401) {
    throw new Error("Authentication required");
  }

  const data = await parseJson<HoldingApiResponse[]>(res, "Failed to fetch holdings");

  return data.map(holding => {
    const rawQuantity = typeof holding.quantity === "string" ? Number(holding.quantity) : holding.quantity;
    return {
      ...holding,
      quantity: Number.isFinite(rawQuantity) ? rawQuantity : 0,
    };
  });
}

type HoldingPayload = {
  symbol: string;
  quantity: number;
};

function holdingsEndpoint(path = "") {
  return `${API_BASE}/portfolio/holdings${path}`;
}

async function authorizedRequest(path: string, init?: RequestInit) {
  const res = await fetch(holdingsEndpoint(path), {
    ...init,
    headers: {
      "Content-Type": "application/json",
      ...authHeader(),
      ...init?.headers,
    },
  });

  if (res.status === 401) {
    throw new Error("Authentication required");
  }

  return res;
}

export async function createHolding(payload: HoldingPayload): Promise<Holding> {
  const res = await authorizedRequest("", {
    method: "POST",
    body: JSON.stringify(payload),
  });

  const data = await parseJson<HoldingApiResponse>(res, "Failed to create holding");
  return {
    ...data,
    quantity: typeof data.quantity === "string" ? Number(data.quantity) : data.quantity,
  };
}

export async function updateHolding(id: number, payload: HoldingPayload): Promise<Holding> {
  const res = await authorizedRequest(`/${id}`, {
    method: "PUT",
    body: JSON.stringify(payload),
  });

  const data = await parseJson<HoldingApiResponse>(res, "Failed to update holding");
  return {
    ...data,
    quantity: typeof data.quantity === "string" ? Number(data.quantity) : data.quantity,
  };
}

export async function deleteHolding(id: number): Promise<void> {
  const res = await authorizedRequest(`/${id}`, {
    method: "DELETE",
  });

  if (!res.ok && res.status !== 204) {
    throw new Error("Failed to delete holding");
  }
}
