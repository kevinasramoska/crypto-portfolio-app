// Quote delimiters and neutralize spreadsheet formulas only in text cells.
export function escapeCsvValue(value: string | number) {
  let text = String(value);
  if (typeof value === "string" && /^[\s\u0000-\u001f]*[=+@-]/u.test(text)) text = `'${text}`;
  return `"${text.replaceAll('"', '""')}"`;
}
