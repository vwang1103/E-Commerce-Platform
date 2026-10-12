import type { Inventory, Product, ProductSummary } from "@/shared/lib/types";

export interface CartLine {
  productId: number;
  name: string;
  categoryId: number;
  unitPriceMinor: number;
  currency: string;
  quantity: number;
}

export const MAX_LINE_QUANTITY = 20;

export const EMPTY_CART: readonly CartLine[] = Object.freeze([]);

export function clampQuantity(quantity: number, max = MAX_LINE_QUANTITY): number {
  if (!Number.isFinite(quantity)) return 1;
  return Math.min(Math.max(1, Math.floor(quantity)), Math.max(1, Math.min(max, MAX_LINE_QUANTITY)));
}

export function toCartLine(product: ProductSummary, quantity = 1): CartLine {
  return {
    productId: product.productId,
    name: product.name,
    categoryId: product.categoryId,
    unitPriceMinor: product.priceMinor,
    currency: product.currency,
    quantity,
  };
}

function isCartLine(value: unknown): value is CartLine {
  if (typeof value !== "object" || value === null) return false;
  const line = value as Record<string, unknown>;
  return (
    Number.isSafeInteger(line.productId) &&
    typeof line.name === "string" &&
    Number.isSafeInteger(line.categoryId) &&
    Number.isSafeInteger(line.unitPriceMinor) &&
    typeof line.currency === "string" &&
    Number.isSafeInteger(line.quantity) &&
    (line.quantity as number) > 0
  );
}

export function parseCart(value: unknown): CartLine[] {
  if (!Array.isArray(value)) return [];
  return value.filter(isCartLine).map((line) => ({ ...line, quantity: clampQuantity(line.quantity) }));
}

export function addLine(lines: readonly CartLine[], line: CartLine): CartLine[] {
  const existing = lines.find((candidate) => candidate.productId === line.productId);
  if (!existing) return [...lines, { ...line, quantity: clampQuantity(line.quantity) }];
  return lines.map((candidate) =>
    candidate.productId === line.productId
      ? { ...line, quantity: clampQuantity(candidate.quantity + line.quantity) }
      : candidate,
  );
}

export function setLineQuantity(lines: readonly CartLine[], productId: number, quantity: number): CartLine[] {
  if (quantity <= 0) return removeLines(lines, [productId]);
  return lines.map((line) => (line.productId === productId ? { ...line, quantity: clampQuantity(quantity) } : line));
}

export function removeLines(lines: readonly CartLine[], productIds: readonly number[]): CartLine[] {
  return lines.filter((line) => !productIds.includes(line.productId));
}

export function cartCount(lines: readonly CartLine[]): number {
  return lines.reduce((sum, line) => sum + line.quantity, 0);
}

export interface PricedLine {
  unitPriceMinor: number;
  currency: string;
  quantity: number;
}

export interface CurrencyTotal {
  currency: string;
  minor: number;
}

/** Orders are priced per currency, so a mixed cart reports one total for each. */
export function cartTotals(lines: readonly PricedLine[]): CurrencyTotal[] {
  const totals = new Map<string, number>();
  for (const line of lines) {
    totals.set(line.currency, (totals.get(line.currency) ?? 0) + line.unitPriceMinor * line.quantity);
  }
  return [...totals].map(([currency, minor]) => ({ currency, minor }));
}

export type LineIssue = { kind: "unavailable" } | { kind: "insufficient"; available: number };

/** `null` means the backend confirmed the record does not exist; `undefined` means it is unknown. */
export interface LineCheck {
  product?: Product | null;
  inventory?: Inventory | null;
}

export interface CheckedLine {
  line: CartLine;
  unitPriceMinor: number;
  currency: string;
  issue?: LineIssue;
  previousPriceMinor?: number;
}

export function checkLine(line: CartLine, check: LineCheck | undefined): CheckedLine {
  const current = check?.product ?? undefined;
  const unitPriceMinor = current?.priceMinor ?? line.unitPriceMinor;
  const result: CheckedLine = {
    line,
    unitPriceMinor,
    currency: current?.currency ?? line.currency,
  };
  if (current && current.priceMinor !== line.unitPriceMinor) result.previousPriceMinor = line.unitPriceMinor;

  if (check?.product === null || (current && current.status !== "ACTIVE") || check?.inventory === null) {
    result.issue = { kind: "unavailable" };
  } else if (check?.inventory && check.inventory.availableQuantity < line.quantity) {
    result.issue = { kind: "insufficient", available: Math.max(0, check.inventory.availableQuantity) };
  }
  return result;
}

export function describeIssue(issue: LineIssue): string {
  if (issue.kind === "unavailable") return "No longer available";
  return issue.available === 0 ? "Out of stock" : `Only ${issue.available} left`;
}
