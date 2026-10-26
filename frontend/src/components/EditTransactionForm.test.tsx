import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import EditTransactionForm from "./EditTransactionForm";

const transaction = { id: 1, symbol: "BTC", name: "Bitcoin", type: "BUY" as const, quantity: 1, priceUsd: 10,
  totalValueUsd: 10, realisedProfitUsd: 0, createdAt: "2026-02-01T12:00:00Z", executedAt: "2026-01-01T12:00:00.123456Z" };

it("preserves the exact execution instant when only price changes", async () => {
  const submit = vi.fn().mockResolvedValue(undefined);
  render(<EditTransactionForm transaction={transaction} onSubmit={submit} onCancel={() => {}} />);
  fireEvent.change(screen.getByLabelText("Price USD"), { target: { value: "11" } });
  fireEvent.click(screen.getByRole("button", { name: "Save changes" }));
  await waitFor(() => expect(submit).toHaveBeenCalledWith(expect.objectContaining({ priceUsd: 11, executedAt: transaction.executedAt })));
});

it("rejects a future execution date without submitting", async () => {
  const submit = vi.fn();
  render(<EditTransactionForm transaction={transaction} onSubmit={submit} onCancel={() => {}} />);
  fireEvent.change(screen.getByLabelText("Execution time (your timezone)"), { target: { value: "2999-01-01T12:00:00" } });
  fireEvent.submit(screen.getByRole("form"));
  expect(screen.getByRole("alert")).toHaveTextContent("Execution time must be between 1970 and now.");
  expect(submit).not.toHaveBeenCalled();
});
