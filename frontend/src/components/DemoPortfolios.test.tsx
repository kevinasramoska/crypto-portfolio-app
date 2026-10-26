import { render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import DemoPortfolios from "./DemoPortfolios";

it("offers three read-only scenarios without auth or API calls", async () => {
  const fetch = vi.spyOn(globalThis, "fetch");
  const user = userEvent.setup();
  render(<DemoPortfolios />);
  expect(screen.getByText("Read-only sample data.")).toBeInTheDocument();
  expect(screen.getByRole("heading", { name: "Long-term builder" })).toBeInTheDocument();
  await user.click(screen.getByRole("button", { name: /Active trader/ }));
  expect(screen.getByRole("heading", { name: "Active trader" })).toBeInTheDocument();
  expect(screen.getByTestId("portfolio-card-realised-p-l")).toHaveTextContent("$6,000.00");
  await user.click(screen.getByRole("button", { name: /Market drawdown/ }));
  expect(screen.getByTestId("portfolio-card-total-p-l")).toHaveTextContent("$8,000.00");
  expect(screen.queryByRole("button", { name: "Edit" })).not.toBeInTheDocument();
  expect(fetch).not.toHaveBeenCalled();
  fetch.mockRestore();
});
