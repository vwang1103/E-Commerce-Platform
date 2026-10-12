import { parseServerDate } from "@/shared/lib/format";
import type { Order, OrderStatus } from "@/shared/lib/types";

const MAX_BATCH = 20;

/** Parses a comma-separated list of order numbers, ignoring anything that is not a plain integer. */
export function parseOrderIds(raw: string | null | undefined): string[] {
  if (!raw) return [];
  const ids = raw.split(",").map((part) => part.trim()).filter((part) => /^\d{1,19}$/.test(part));
  return [...new Set(ids)].slice(0, MAX_BATCH);
}

export function earliestDeadline(orders: readonly Order[]): string | undefined {
  return orders
    .filter((order) => order.status === "PENDING_PAYMENT")
    .map((order) => order.paymentDeadline)
    .sort((a, b) => parseServerDate(a).getTime() - parseServerDate(b).getTime())[0];
}

export type OrderTab = "all" | "pending" | "paid" | "closed";

export const ORDER_TABS: ReadonlyArray<{ id: OrderTab; label: string }> = [
  { id: "all", label: "All" },
  { id: "pending", label: "Awaiting payment" },
  { id: "paid", label: "Paid" },
  { id: "closed", label: "Closed" },
];

const TAB_STATUSES: Record<Exclude<OrderTab, "all">, readonly OrderStatus[]> = {
  pending: ["ACCEPTED", "PENDING_PAYMENT", "CONFIRMING_INVENTORY"],
  paid: ["PAID"],
  closed: ["EXPIRED", "REJECTED", "FAILED", "INVENTORY_CONFIRMATION_FAILED"],
};

export function isOrderTab(value: string | null | undefined): value is OrderTab {
  return ORDER_TABS.some((tab) => tab.id === value);
}

export function matchesTab(order: Order, tab: OrderTab): boolean {
  return tab === "all" || TAB_STATUSES[tab].includes(order.status);
}

export function summarizeItems(order: Order): string {
  const [first, ...rest] = order.items;
  if (!first) return "Order";
  return rest.length === 0 ? first.productName : `${first.productName} and ${rest.length} more`;
}
