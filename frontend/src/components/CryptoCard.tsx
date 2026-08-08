type Props = {
  symbol: string;
  price?: number;
  loading?: boolean;
  currency?: string;
  onRemove?: () => void;
};

export default function CryptoCard({ symbol, price, loading, currency = "EUR", onRemove }: Props) {
  const hasPrice = typeof price === "number" && !Number.isNaN(price);
  const formatter = new Intl.NumberFormat("en-US", {
    style: "currency",
    currency,
    maximumFractionDigits: 2,
  });

  return (
    <div className="relative overflow-hidden rounded-xl border border-gray-800 bg-gray-950/70 p-5 text-white shadow-sm transition duration-200 hover:-translate-y-0.5 hover:border-purple-500/40 hover:shadow-lg hover:shadow-purple-950/20">
      <div className="flex items-center justify-between">
        <div>
          <p className="text-[11px] font-semibold uppercase tracking-[0.16em] text-gray-500">Live market</p>
          <h2 className="mt-1 text-xl font-bold">{symbol}</h2>
        </div>
        {onRemove && (
          <button
            type="button"
            onClick={onRemove}
            className="rounded-full border border-gray-700 px-2 py-1 text-xs uppercase tracking-wide text-gray-400 hover:border-red-500 hover:text-red-400 transition"
            aria-label={`Remove ${symbol}`}
          >
            Remove
          </button>
        )}
      </div>
      {loading ? (
        <div className="mt-4 h-8 w-32 rounded-sm bg-gray-800 animate-pulse" />
      ) : hasPrice ? (
        <p className="mt-4 text-3xl font-semibold tracking-tight text-purple-200 tabular-nums">{formatter.format(price)}</p>
      ) : (
        <p className="mt-4 text-sm font-medium text-amber-300">No market data</p>
      )}
    </div>
  );
}
