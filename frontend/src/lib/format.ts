export function formatUnitPrice(value: number, currency = "USD") {
  if (value !== 0 && Math.abs(value) < 0.000000000001) return `${currency} ${value.toPrecision(4)}`;
  return new Intl.NumberFormat("en-US", {
    style: "currency", currency, minimumFractionDigits: 2,
    maximumFractionDigits: Math.abs(value) < 1 ? 12 : 8,
  }).format(value);
}
