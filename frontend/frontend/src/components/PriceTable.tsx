import { Holding, PriceMap } from "@/lib/types";

type Props = {
  holdings: Holding[];
  prices: PriceMap;
  loading?: boolean;
  error?: string | null;
  onDelete?: (id: number) => void;
  deletingId?: number | null;
  onEdit?: (holding: Holding) => void;
  editingId?: number | null;
  emptyMessage?: string;
};

const currencyFormatter = new Intl.NumberFormat("en-US", {
  style: "currency",
  currency: "EUR",
  maximumFractionDigits: 2,
});

function formatQuantity(quantity: number) {
  const formatted = quantity.toLocaleString(undefined, {
    minimumFractionDigits: 0,
    maximumFractionDigits: 12,
  });

  if (formatted === "0" && quantity !== 0) {
    return quantity.toPrecision(6);
  }

  return formatted;
}

export default function PriceTable({
  holdings,
  prices,
  loading,
  error,
  onDelete,
  deletingId,
  onEdit,
  editingId,
  emptyMessage = "You haven't added any holdings yet.",
}: Props) {
  if (loading) {
    return (
      <div className="mt-10 space-y-3">
        {[...Array(3)].map((_, index) => (
          <div key={index} className="h-14 rounded-lg bg-gray-900 border border-gray-800 animate-pulse" />
        ))}
      </div>
    );
  }

  if (error) {
    return (
      <div className="mt-10 rounded-xl border border-red-500/40 bg-red-500/10 p-6 text-sm text-red-200">
        {error}
      </div>
    );
  }

  if (!holdings.length) {
    return (
      <div className="mt-10 rounded-xl border border-gray-800 bg-gray-900/60 p-6 text-sm text-gray-400">
        {emptyMessage}
      </div>
    );
  }

  const rows = holdings.map(holding => {
    const price = prices[holding.symbol.toUpperCase()];
    const hasPrice = typeof price === "number" && !Number.isNaN(price);
    const value = hasPrice ? price * holding.quantity : null;

    return { ...holding, price: hasPrice ? price : null, value };
  });

  const totalValue = rows.reduce((acc, holding) => acc + (holding.value ?? 0), 0);
  const showActions = typeof onDelete === "function" || typeof onEdit === "function";

  return (
    <div className="mt-10 overflow-hidden rounded-xl border border-gray-800">
      <table className="w-full table-auto">
        <thead className="bg-gray-900/80 text-left text-xs uppercase tracking-wider text-gray-400">
          <tr>
            <th className="px-6 py-3 font-semibold">Asset</th>
            <th className="px-6 py-3 font-semibold">Quantity</th>
            <th className="px-6 py-3 font-semibold">Price</th>
            <th className="px-6 py-3 font-semibold text-right">Value</th>
            {showActions && <th className="px-6 py-3 font-semibold text-right">Actions</th>}
          </tr>
        </thead>
        <tbody className="bg-gray-950/40 text-sm text-gray-100">
          {rows.map(row => (
            <tr key={row.id} className="border-t border-gray-900/40">
              <td className="px-6 py-4">
                <div className="font-semibold">{row.symbol}</div>
                <div className="text-xs text-gray-500">{row.name}</div>
              </td>
              <td className="px-6 py-4">{formatQuantity(row.quantity)}</td>
              <td className="px-6 py-4">
                {row.price !== null ? currencyFormatter.format(row.price) : <span className="text-gray-500">—</span>}
              </td>
              <td className="px-6 py-4 text-right">
                {row.value !== null ? currencyFormatter.format(row.value) : <span className="text-gray-500">No price</span>}
              </td>
              {showActions && (
                <td className="px-6 py-4 text-right">
                  <div className="flex items-center justify-end gap-2">
                    {typeof onEdit === "function" && (
                      <button
                        onClick={() => onEdit(row)}
                        disabled={editingId === row.id}
                        className="rounded-lg border border-purple-500/50 px-4 py-1.5 text-sm text-purple-200 hover:bg-purple-500/10 transition disabled:cursor-not-allowed disabled:opacity-50"
                      >
                        {editingId === row.id ? "Editing…" : "Edit"}
                      </button>
                    )}
                    {typeof onDelete === "function" && (
                      <button
                        onClick={() => onDelete(row.id)}
                        disabled={deletingId === row.id}
                        className="rounded-lg border border-red-500/50 px-4 py-1.5 text-sm text-red-300 hover:bg-red-500/10 transition disabled:cursor-not-allowed disabled:opacity-50"
                      >
                        {deletingId === row.id ? "Removing…" : "Remove"}
                      </button>
                    )}
                  </div>
                </td>
              )}
            </tr>
          ))}
        </tbody>
        <tfoot className="bg-gray-900/80 text-sm font-semibold text-gray-100">
          <tr>
            <td className="px-6 py-4" colSpan={3}>
              Total portfolio value
            </td>
            <td className="px-6 py-4 text-right">{currencyFormatter.format(totalValue)}</td>
            {showActions && <td className="px-6 py-4" />}
          </tr>
        </tfoot>
      </table>
    </div>
  );
}
