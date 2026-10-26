import { executionTimeError, toLocalDateTimeInput } from "./dates";

it("allows an omitted/past execution time and rejects invalid or future values", () => {
  expect(executionTimeError("")).toBeNull();
  expect(executionTimeError("2026-01-01T12:00:00Z")).toBeNull();
  expect(executionTimeError("invalid")).toBeTruthy();
  expect(executionTimeError("2999-01-01T00:00:00Z")).toBeTruthy();
  expect(executionTimeError("1969-01-01T00:00:00Z")).toBeTruthy();
});

it("round-trips local inputs while handling invalid legacy dates", () => {
  const original = "2026-01-01T12:30:45Z";
  expect(new Date(toLocalDateTimeInput(original)).toISOString()).toBe("2026-01-01T12:30:45.000Z");
  expect(toLocalDateTimeInput("invalid")).toBe("");
});
