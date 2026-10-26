/** datetime-local has no timezone; the browser interprets it in the user's local zone. */
export function toLocalDateTimeInput(value: string) {
  const date = new Date(value);
  if (!Number.isFinite(date.getTime())) return "";
  const local = new Date(date.getTime() - date.getTimezoneOffset() * 60_000);
  return local.toISOString().slice(0, 19);
}

export function executionTimeError(value: string) {
  if (!value) return null;
  const time = new Date(value).getTime();
  return !Number.isFinite(time) || time < 0 || time > Date.now()
    ? "Execution time must be between 1970 and now." : null;
}
