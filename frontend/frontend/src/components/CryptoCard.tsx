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
    <div className="relative rounded-xl bg-gray-900 text-white p-5 border border-gray-700 shadow">
      <div className="flex items-center justify-between">
        <h2 className="text-xl font-bold">{symbol}</h2>
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
        <div className="mt-4 h-8 w-32 rounded bg-gray-800 animate-pulse" />
      ) : hasPrice ? (
        <p className="text-3xl mt-2 text-purple-300">{formatter.format(price)}</p>
      ) : (
        <p className="text-sm mt-2 text-red-400">No data</p>
      )}
    </div>
  );
}
