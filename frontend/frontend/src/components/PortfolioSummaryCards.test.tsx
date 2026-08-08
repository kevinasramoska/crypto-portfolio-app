import { render, screen } from "@testing-library/react";
import PortfolioSummaryCards from "@/components/PortfolioSummaryCards";
import { PortfolioSummary } from "@/lib/types";

const summary: PortfolioSummary = {
  totalCurrentValueUsd: 1700,
  totalInvestedUsd: 1500,
  totalUnrealisedProfitLossUsd: -100,
  totalRealisedProfitLossUsd: 300,
  totalProfitLossUsd: 200,
  holdings: [],
  hasUnsupportedMarketData: false,
};

describe("PortfolioSummaryCards", () => {
  it("shows signed profit and loss values in the breakdown", () => {
    render(<PortfolioSummaryCards summary={summary} />);

    expect(screen.getByText("+$300.00")).toBeInTheDocument();
    expect(screen.getAllByText("-$100.00")).toHaveLength(2);
    expect(screen.getByRole("img", { name: "Realised +$300.00. Unrealised -$100.00." })).toBeInTheDocument();
  });

  it("shows a helpful empty state when summary data is unavailable", () => {
    render(<PortfolioSummaryCards summary={null} />);

    expect(screen.getByText("Your profit and loss breakdown will appear once portfolio data is available.")).toBeInTheDocument();
  });
});
