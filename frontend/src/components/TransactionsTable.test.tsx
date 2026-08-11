import { render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import TransactionsTable from "@/components/TransactionsTable";
import { Transaction } from "@/lib/types";

const transaction: Transaction = {
  id: 11,
  symbol: "BTC",
  name: "Bitcoin",
  type: "BUY",
  quantity: 1,
  priceUsd: 50000,
  totalValueUsd: 50000,
  realisedProfitUsd: 0,
  createdAt: "2026-08-08T12:00:00Z",
};

describe("TransactionsTable", () => {
  it("submits an edit for the active transaction id", async () => {
    const user = userEvent.setup();
    const onEdit = vi.fn().mockResolvedValue(undefined);

    render(<TransactionsTable transactions={[transaction]} onEdit={onEdit} />);

    await user.click(screen.getByRole("button", { name: "Edit" }));
    const priceInput = screen.getByRole("spinbutton", { name: "Price USD" });
    await user.clear(priceInput);
    await user.type(priceInput, "55000");
    await user.click(screen.getByRole("button", { name: "Save changes" }));

    expect(onEdit).toHaveBeenCalledWith(11, {
      symbol: "BTC",
      name: "Bitcoin",
      type: "BUY",
      quantity: 1,
      priceUsd: 55000,
    });
    expect(screen.queryByRole("form", { name: "Edit BTC transaction" })).not.toBeInTheDocument();
  });

  it("requires confirmation before deleting a transaction", async () => {
    const user = userEvent.setup();
    const onDelete = vi.fn().mockResolvedValue(undefined);
    vi.spyOn(window, "confirm").mockReturnValue(true);

    render(<TransactionsTable transactions={[transaction]} onDelete={onDelete} />);

    await user.click(screen.getByRole("button", { name: "Delete" }));

    expect(window.confirm).toHaveBeenCalledWith("Delete the BUY transaction for BTC?");
    expect(onDelete).toHaveBeenCalledWith(11);
  });
});
