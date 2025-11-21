"use client";

type SortKey = "alphabetical" | "quantity" | "value";

type Props = {
  filter: string;
  sort: SortKey;
  onFilterChange: (value: string) => void;
  onSortChange: (value: SortKey) => void;
  disabled?: boolean;
};

const SORT_OPTIONS: { label: string; value: SortKey }[] = [
  { label: "Symbol (A → Z)", value: "alphabetical" },
  { label: "Quantity (high → low)", value: "quantity" },
  { label: "Value (high → low)", value: "value" },
];

export default function HoldingsControls({ filter, sort, onFilterChange, onSortChange, disabled }: Props) {
  return (
    <div className="flex flex-col gap-4 rounded-xl border border-gray-800 bg-gray-950/40 p-4 sm:flex-row sm:items-center sm:justify-between">
      <label className="flex-1">
        <span className="text-xs uppercase tracking-wide text-gray-500">Filter by symbol</span>
        <input
          type="text"
          placeholder="e.g. BTC"
          className="mt-2 w-full rounded-lg border border-gray-800 bg-gray-950/70 px-4 py-2.5 text-sm text-white placeholder:text-gray-500 focus:border-purple-500 focus:outline-none"
          value={filter}
          onChange={event => onFilterChange(event.target.value)}
          disabled={disabled}
        />
      </label>

      <label className="sm:w-60">
        <span className="text-xs uppercase tracking-wide text-gray-500">Sort holdings</span>
        <select
          className="mt-2 w-full rounded-lg border border-gray-800 bg-gray-950/70 px-4 py-2.5 text-sm text-white focus:border-purple-500 focus:outline-none"
          value={sort}
          onChange={event => onSortChange(event.target.value as SortKey)}
          disabled={disabled}
        >
          {SORT_OPTIONS.map(option => (
            <option key={option.value} value={option.value}>
              {option.label}
            </option>
          ))}
        </select>
      </label>
    </div>
  );
}

export type { SortKey };
