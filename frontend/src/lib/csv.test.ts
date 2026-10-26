import { escapeCsvValue } from "./csv";
import { formatUnitPrice } from "./format";

describe("export and unit prices", () => {
  it.each(["=1+1", "+cmd", "-formula", "@SUM(1)", " \t=1+1", "\r=1"])("neutralizes %j", value => {
    expect(escapeCsvValue(value)).toMatch(/^"'/);
  });
  it("retains negative numeric profit and escapes delimiters", () => {
    expect(escapeCsvValue(-12.5)).toBe('"-12.5"');
    expect(escapeCsvValue('A,"B"')).toBe('"A,""B"""');
  });
  it("does not display sub-cent unit prices as zero", () => {
    expect(formatUnitPrice(0.0049)).toBe("$0.0049");
    expect(formatUnitPrice(0.00000001)).toBe("$0.00000001");
  });
});
