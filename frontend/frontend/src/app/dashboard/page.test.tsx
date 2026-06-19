import { render, screen, waitFor } from "@testing-library/react";
import DashboardPage from "@/app/dashboard/page";

const apiMocks = vi.hoisted(() => ({
  createTransaction: vi.fn(),
  getHoldings: vi.fn(),
  getPortfolioPerformanceHistory: vi.fn(),
  getPortfolioSummary: vi.fn(),
  getPrices: vi.fn(),
  getTransactions: vi.fn(),
  getTransactionSummary: vi.fn(),
  getTransactionsPaginated: vi.fn(),
}));

vi.mock("@/lib/api", () => ({
  ...apiMocks,
  AUTH_REQUIRED_MESSAGE: "Authentication required",
}));

vi.mock("@/components/AddTransactionForm", () => ({
  default: () => <div>Add Transaction Form</div>,
}));
vi.mock("@/components/AssetAllocationSummary", () => ({
  default: () => <div>Asset Allocation Summary</div>,
}));
vi.mock("@/components/CryptoCard", () => ({
  default: ({ symbol }: { symbol: string }) => <div>{symbol} card</div>,
}));
vi.mock("@/components/HoldingsControls", () => ({
  default: () => <div>Holdings Controls</div>,
}));
vi.mock("@/components/PerformanceHistory", () => ({
  default: () => <div>Performance History</div>,
}));
vi.mock("@/components/PortfolioSummaryCards", () => ({
  default: () => <div>Portfolio Summary Cards</div>,
}));
vi.mock("@/components/PriceTable", () => ({
  default: () => <div>Price Table</div>,
}));
vi.mock("@/components/TransactionSummaryCards", () => ({
  default: () => <div>Transaction Summary Cards</div>,
}));
vi.mock("@/components/TransactionsTable", () => ({
  default: () => <div>Transactions Table</div>,
}));
vi.mock("@/components/WatchlistControls", () => ({
  default: () => <div>Watchlist Controls</div>,
}));

describe("DashboardPage", () => {
  beforeEach(() => {
    vi.clearAllMocks();
    apiMocks.getPrices.mockResolvedValue({ BTC: 65000, ETH: 3200, SOL: 150, LINK: 18, DOGE: 0.14 });
    apiMocks.getHoldings.mockResolvedValue([]);
    apiMocks.getPortfolioSummary.mockResolvedValue(null);
    apiMocks.getTransactionsPaginated.mockResolvedValue({
      content: [],
      pageNumber: 0,
      pageSize: 20,
      totalElements: 0,
      totalPages: 0,
      hasNext: false,
      hasPrevious: false,
    });
    apiMocks.getTransactionSummary.mockResolvedValue(null);
    apiMocks.getPortfolioPerformanceHistory.mockResolvedValue({ range: "30d", history: [] });
    apiMocks.getTransactions.mockResolvedValue([]);
    apiMocks.createTransaction.mockResolvedValue({});
    window.localStorage.clear();
  });

  it("renders the dashboard shell and loads core data", async () => {
    render(<DashboardPage />);

    expect(screen.getByRole("heading", { name: "Dashboard" })).toBeInTheDocument();
    expect(screen.getByText("Portfolio summary")).toBeInTheDocument();
    expect(screen.getByText("Transactions")).toBeInTheDocument();

    await waitFor(() => {
      expect(apiMocks.getPortfolioSummary).toHaveBeenCalled();
      expect(apiMocks.getTransactionsPaginated).toHaveBeenCalled();
      expect(apiMocks.getPortfolioPerformanceHistory).toHaveBeenCalledWith("30d");
    });
  });
});
