import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import AddTransactionForm from "@/components/AddTransactionForm";

const getSupportedCoinsMock = vi.fn();

vi.mock("@/lib/api", () => ({
  getSupportedCoins: () => getSupportedCoinsMock(),
}));

describe("AddTransactionForm", () => {
  beforeEach(() => {
    getSupportedCoinsMock.mockReset();
    getSupportedCoinsMock.mockResolvedValue([]);
  });

  it.each(["", "0"])("requires an explicit price (input %j)", async price => {
    const onSubmit = vi.fn().mockResolvedValue(undefined);
    render(<AddTransactionForm onSubmit={onSubmit} />);
    fireEvent.change(screen.getByLabelText("Symbol"), { target: { value: "BTC" } });
    fireEvent.change(screen.getByLabelText("Name"), { target: { value: "Bitcoin" } });
    fireEvent.change(screen.getByLabelText("Quantity"), { target: { value: "100" } });
    fireEvent.change(screen.getByLabelText("Price USD"), { target: { value: price } });
    fireEvent.click(screen.getByRole("button", { name: "Save transaction" }));
    if (!price) {
      expect(await screen.findByText(/Price is required/)).toBeInTheDocument();
      expect(onSubmit).not.toHaveBeenCalled();
    } else {
      await waitFor(() => expect(onSubmit).toHaveBeenCalledWith(expect.objectContaining({ priceUsd: 0 })));
    }
  });

  it("renders the transaction form shell", () => {
    render(<AddTransactionForm onSubmit={vi.fn()} />);

    expect(screen.getByRole("heading", { name: "Record buy or sell" })).toBeInTheDocument();
    expect(screen.getByRole("button", { name: "Save transaction" })).toBeInTheDocument();
  });

  it("shows validation errors for invalid manual input", async () => {
    render(<AddTransactionForm onSubmit={vi.fn()} />);

    fireEvent.change(screen.getByLabelText("Symbol"), { target: { value: "B" } });
    fireEvent.change(screen.getByLabelText("Name"), { target: { value: "" } });
    fireEvent.change(screen.getByLabelText("Quantity"), { target: { value: "0" } });
    fireEvent.change(screen.getByLabelText("Price USD"), { target: { value: "-1" } });
    fireEvent.click(screen.getByRole("button", { name: "Save transaction" }));

    expect(await screen.findByText("Symbol must use 2-10 letters or numbers.")).toBeInTheDocument();
  });

  it("submits normalized values for a valid transaction", async () => {
    const onSubmit = vi.fn().mockResolvedValue(undefined);

    render(<AddTransactionForm onSubmit={onSubmit} />);

    fireEvent.change(screen.getByLabelText("Symbol"), { target: { value: "btc" } });
    fireEvent.change(screen.getByLabelText("Name"), { target: { value: "Bitcoin" } });
    fireEvent.change(screen.getByLabelText("Quantity"), { target: { value: "0.5" } });
    fireEvent.change(screen.getByLabelText("Price USD"), { target: { value: "65000" } });
    fireEvent.click(screen.getByRole("button", { name: "Save transaction" }));

    await waitFor(() => {
      expect(onSubmit).toHaveBeenCalledWith({
        symbol: "BTC",
        name: "Bitcoin",
        type: "BUY",
        quantity: 0.5,
        priceUsd: 65000,
      });
    });
  });
});
