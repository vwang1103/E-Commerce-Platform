import { render, screen } from "@testing-library/react";
import { describe, expect, it } from "vitest";
import { OrderStatusBadge } from "@/features/orders/components/OrderStatusBadge";
import { OrderTimeline, timelineSteps } from "@/features/orders/components/OrderTimeline";

describe("OrderStatusBadge", () => {
  it("presents a readable pending payment state", () => {
    render(<OrderStatusBadge status="PENDING_PAYMENT" />);
    expect(screen.getByText("Awaiting payment")).toHaveClass("status-pending-payment");
  });

  it("uses customer-friendly labels for terminal states", () => {
    render(<OrderStatusBadge status="REJECTED" />);
    expect(screen.getByText("Unavailable")).toBeInTheDocument();
  });
});

describe("OrderTimeline", () => {
  it("marks the payment step as current while awaiting payment", () => {
    expect(timelineSteps("PENDING_PAYMENT").map((step) => step.state)).toEqual(["done", "done", "current"]);
  });

  it("shows a failed final step for expired orders", () => {
    expect(timelineSteps("EXPIRED").at(-1)).toEqual({ label: "Expired", state: "failed" });
  });

  it("exposes progress to assistive technology", () => {
    render(<OrderTimeline status="PAID" />);
    expect(screen.getByRole("list", { name: "Order progress" })).toBeInTheDocument();
    expect(screen.getByText(/Paid/)).toBeInTheDocument();
  });
});
