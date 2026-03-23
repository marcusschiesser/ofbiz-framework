import { render, screen } from "@testing-library/react";
import { describe, expect, it } from "vitest";
import { StatusBadge } from "@/components/orders/status-badge";

describe("StatusBadge", () => {
  it("renders the lifecycle status label", () => {
    render(<StatusBadge status="SHIPPED" />);
    expect(screen.getByText("SHIPPED")).toBeVisible();
  });
});
