import {
  AUTH_REQUIRED_MESSAGE,
  createTransaction,
  deleteTransaction,
  getHoldings,
  getPortfolioSummary,
  updateTransaction,
} from "@/lib/api";

const authHeaderMock = vi.fn();
const clearAuthSessionMock = vi.fn();

vi.mock("@/lib/auth", () => ({
  authHeader: () => authHeaderMock(),
  clearAuthSession: () => clearAuthSessionMock(),
}));

describe("api client", () => {
  beforeEach(() => {
    authHeaderMock.mockReset();
    clearAuthSessionMock.mockReset();
    authHeaderMock.mockReturnValue({});
    vi.restoreAllMocks();
  });

  it("clears auth session and throws a stable auth error on 401", async () => {
    authHeaderMock.mockReturnValue({ Authorization: "Bearer test-token" });
    vi.spyOn(global, "fetch").mockResolvedValue(
      new Response(null, {
        status: 401,
      })
    );

    await expect(getPortfolioSummary()).rejects.toThrow(AUTH_REQUIRED_MESSAGE);

    expect(clearAuthSessionMock).toHaveBeenCalledTimes(1);
    expect(global.fetch).toHaveBeenCalledWith(
      "http://localhost:8080/api/portfolio/summary",
      expect.objectContaining({
        headers: expect.any(Headers),
      })
    );
    const headers = (vi.mocked(global.fetch).mock.calls[0]?.[1] as RequestInit).headers as Headers;
    expect(headers.get("Authorization")).toBe("Bearer test-token");
  });

  it("parses holdings and coerces numeric string fields", async () => {
    authHeaderMock.mockReturnValue({ Authorization: "Bearer test-token" });
    vi.spyOn(global, "fetch").mockResolvedValue(
      new Response(
        JSON.stringify([
          {
            id: 1,
            symbol: "BTC",
            name: "Bitcoin",
            quantity: "0.50000000",
            averageBuyPriceUsd: "50000.25",
            currentPriceUsd: "65000.75",
            investedValueUsd: "25000.12",
            currentValueUsd: "32500.38",
            profitLossUsd: "7500.26",
            marketPriceAvailable: true,
          },
        ]),
        {
          status: 200,
          headers: { "Content-Type": "application/json" },
        }
      )
    );

    const holdings = await getHoldings();

    expect(holdings).toEqual([
      {
        id: 1,
        symbol: "BTC",
        name: "Bitcoin",
        quantity: 0.5,
        averageBuyPriceUsd: 50000.25,
        currentPriceUsd: 65000.75,
        investedValueUsd: 25000.12,
        currentValueUsd: 32500.38,
        profitLossUsd: 7500.26,
        marketPriceAvailable: true,
      },
    ]);
  });

  it("uses backend error details for failed transaction creation", async () => {
    authHeaderMock.mockReturnValue({ Authorization: "Bearer test-token" });
    vi.spyOn(global, "fetch").mockResolvedValue(
      new Response(
        JSON.stringify({
          error: "Insufficient holdings for SELL transaction",
        }),
        {
          status: 400,
          headers: { "Content-Type": "application/json" },
        }
      )
    );

    await expect(
      createTransaction({
        symbol: "BTC",
        name: "Bitcoin",
        type: "SELL",
        quantity: 2,
        priceUsd: 65000,
      })
    ).rejects.toThrow("Insufficient holdings for SELL transaction");
  });

  it("falls back to the default error message when the error body is not json", async () => {
    authHeaderMock.mockReturnValue({ Authorization: "Bearer test-token" });
    vi.spyOn(global, "fetch").mockResolvedValue(
      new Response("server exploded", {
        status: 500,
        headers: { "Content-Type": "text/plain" },
      })
    );

    await expect(
      createTransaction({
        symbol: "BTC",
        name: "Bitcoin",
        type: "BUY",
        quantity: 1,
        priceUsd: 65000,
      })
    ).rejects.toThrow("Failed to create transaction");
  });

  it("updates a transaction using PUT and returns the replacement", async () => {
    authHeaderMock.mockReturnValue({ Authorization: "Bearer test-token" });
    vi.spyOn(global, "fetch").mockResolvedValue(
      new Response(JSON.stringify({ id: 12, symbol: "BTC" }), {
        status: 200,
        headers: { "Content-Type": "application/json" },
      })
    );

    const payload = {
      symbol: "BTC",
      name: "Bitcoin",
      type: "BUY" as const,
      quantity: 1,
      priceUsd: 55000,
    };
    await updateTransaction(11, payload);

    expect(global.fetch).toHaveBeenCalledWith(
      "http://localhost:8080/api/transactions/11",
      expect.objectContaining({ method: "PUT", body: JSON.stringify(payload) })
    );
  });

  it("deletes a transaction using DELETE without parsing the empty response", async () => {
    authHeaderMock.mockReturnValue({ Authorization: "Bearer test-token" });
    vi.spyOn(global, "fetch").mockResolvedValue(new Response(null, { status: 204 }));

    await expect(deleteTransaction(11)).resolves.toBeUndefined();
    expect(global.fetch).toHaveBeenCalledWith(
      "http://localhost:8080/api/transactions/11",
      expect.objectContaining({ method: "DELETE" })
    );
  });
});
