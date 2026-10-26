import { fireEvent, render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import TransactionHistoryControls from "./TransactionHistoryControls";

it("converts inclusive UTC dates to exclusive API bounds and normalizes symbols", async () => {
  const apply = vi.fn();
  const user = userEvent.setup();
  render(<TransactionHistoryControls onApply={apply} />);
  await user.type(screen.getByLabelText("History symbol"), "btc");
  fireEvent.change(screen.getByLabelText("From date (UTC)"), { target: { value: "2026-01-01" } });
  fireEvent.change(screen.getByLabelText("Through date (UTC)"), { target: { value: "2026-01-02" } });
  await user.click(screen.getByRole("button", { name: "Apply filters" }));
  expect(apply).toHaveBeenCalledWith(expect.objectContaining({ symbol: "BTC", from: "2026-01-01T00:00:00.000Z", to: "2026-01-03T00:00:00.000Z" }));
  await user.click(screen.getByRole("button", { name: "Reset filters" }));
  expect(apply).toHaveBeenLastCalledWith({ sort: "executedAt", direction: "desc" });
});

it("rejects a reversed date interval without sending a request", async () => {
  const apply = vi.fn();
  render(<TransactionHistoryControls onApply={apply} />);
  fireEvent.change(screen.getByLabelText("From date (UTC)"), { target: { value: "2026-02-01" } });
  fireEvent.change(screen.getByLabelText("Through date (UTC)"), { target: { value: "2026-01-01" } });
  fireEvent.submit(screen.getByRole("form"));
  expect(screen.getByRole("alert")).toHaveTextContent("Start date must not be after end date.");
  expect(apply).not.toHaveBeenCalled();
});
