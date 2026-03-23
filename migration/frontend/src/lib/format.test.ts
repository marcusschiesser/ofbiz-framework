import { describe, expect, it } from "vitest";
import { formatCurrency, formatDateTime } from "@/lib/format";

describe("format helpers", () => {
  it("formats currency values", () => {
    expect(formatCurrency(49.9, "USD")).toBe("$49.90");
  });

  it("formats timestamps", () => {
    expect(formatDateTime("2026-02-01T09:30:00Z")).toContain("2026");
  });
});
