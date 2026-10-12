import { describe, expect, it } from "vitest";
import {
  addLine,
  cartCount,
  cartTotals,
  checkLine,
  clampQuantity,
  describeIssue,
  parseCart,
  removeLines,
  setLineQuantity,
  type CartLine,
} from "@/features/cart/lib/cart";
import type { Inventory, Product } from "@/shared/lib/types";

const line = (overrides: Partial<CartLine> = {}): CartLine => ({
  productId: 1,
  name: "Headphones",
  categoryId: 100,
  unitPriceMinor: 14900,
  currency: "USD",
  quantity: 1,
  ...overrides,
});

const product = (overrides: Partial<Product> = {}): Product => ({
  productId: 1,
  userId: "aaaaaaaa-aaaa-4aaa-8aaa-aaaaaaaaaaaa",
  categoryId: 100,
  name: "Headphones",
  description: "d",
  priceMinor: 14900,
  currency: "USD",
  status: "ACTIVE",
  version: 0,
  ...overrides,
});

const inventory = (availableQuantity: number): Inventory => ({
  productId: 1,
  totalQuantity: 50,
  availableQuantity,
  reservedQuantity: 0,
  soldQuantity: 0,
  version: 0,
});

describe("cart lines", () => {
  it("merges repeated products and caps the quantity", () => {
    const merged = addLine([line({ quantity: 19 })], line({ quantity: 5 }));
    expect(merged).toHaveLength(1);
    expect(merged[0].quantity).toBe(20);
  });

  it("removes a line when its quantity reaches zero", () => {
    expect(setLineQuantity([line()], 1, 0)).toEqual([]);
    expect(setLineQuantity([line()], 1, 3)[0].quantity).toBe(3);
  });

  it("removes only the requested products", () => {
    const lines = [line(), line({ productId: 2 })];
    expect(removeLines(lines, [1]).map((entry) => entry.productId)).toEqual([2]);
  });

  it("clamps invalid quantities", () => {
    expect(clampQuantity(Number.NaN)).toBe(1);
    expect(clampQuantity(0)).toBe(1);
    expect(clampQuantity(500)).toBe(20);
    expect(clampQuantity(9, 4)).toBe(4);
  });

  it("drops malformed stored lines", () => {
    expect(parseCart("nope")).toEqual([]);
    expect(parseCart([line(), { productId: "bad" }, null])).toHaveLength(1);
  });
});

describe("cart totals", () => {
  it("counts units and totals per currency", () => {
    const lines = [line({ quantity: 2 }), line({ productId: 2, unitPriceMinor: 500, quantity: 1 }), line({ productId: 3, currency: "EUR", unitPriceMinor: 100 })];
    expect(cartCount(lines)).toBe(4);
    expect(cartTotals(lines)).toEqual([
      { currency: "USD", minor: 30300 },
      { currency: "EUR", minor: 100 },
    ]);
  });
});

describe("checkLine", () => {
  it("reports nothing when the check is unknown", () => {
    expect(checkLine(line(), undefined).issue).toBeUndefined();
  });

  it("uses the current catalog price and notes the change", () => {
    const checked = checkLine(line(), { product: product({ priceMinor: 12900 }), inventory: inventory(10) });
    expect(checked.unitPriceMinor).toBe(12900);
    expect(checked.previousPriceMinor).toBe(14900);
    expect(checked.issue).toBeUndefined();
  });

  it("flags products that are gone or inactive", () => {
    expect(checkLine(line(), { product: null }).issue).toEqual({ kind: "unavailable" });
    expect(checkLine(line(), { product: product({ status: "INACTIVE" }) }).issue).toEqual({ kind: "unavailable" });
    expect(checkLine(line(), { inventory: null }).issue).toEqual({ kind: "unavailable" });
  });

  it("flags quantities above the available stock", () => {
    const issue = checkLine(line({ quantity: 3 }), { inventory: inventory(2) }).issue;
    expect(issue).toEqual({ kind: "insufficient", available: 2 });
    expect(describeIssue(issue!)).toBe("Only 2 left");
    expect(describeIssue({ kind: "insufficient", available: 0 })).toBe("Out of stock");
  });
});
