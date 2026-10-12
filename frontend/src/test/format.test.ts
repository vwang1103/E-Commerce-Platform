import { describe, expect, it } from "vitest";
import { formatCountdown, formatMoney, parseServerDate, pluralize, shortOrderNumber } from "@/shared/lib/format";
import { parseOrderIds, summarizeItems } from "@/features/orders/lib/orders";
import { minorToInput, parsePriceToMinor, validateProductForm, EMPTY_FORM } from "@/features/seller/lib/seller";
import type { Order } from "@/shared/lib/types";

describe("formatting", () => {
  it("formats money from minor units", () => {
    expect(formatMoney(129900, "USD")).toBe("$1,299.00");
  });

  it("treats offset-less backend timestamps as UTC", () => {
    expect(parseServerDate("2026-10-11T10:15:00").toISOString()).toBe("2026-10-11T10:15:00.000Z");
    expect(parseServerDate("2026-10-11T10:15:00Z").toISOString()).toBe("2026-10-11T10:15:00.000Z");
    expect(parseServerDate("2026-10-11T12:15:00+02:00").toISOString()).toBe("2026-10-11T10:15:00.000Z");
  });

  it("formats countdowns", () => {
    expect(formatCountdown(0)).toBe("0:00");
    expect(formatCountdown(61_000)).toBe("1:01");
    expect(formatCountdown(-5_000)).toBe("0:00");
    expect(formatCountdown(3_725_000)).toBe("1:02:05");
  });

  it("shortens long order numbers and pluralizes", () => {
    expect(shortOrderNumber("102643349604794368")).toBe("…04794368");
    expect(shortOrderNumber("1234")).toBe("1234");
    expect(pluralize(1, "item")).toBe("1 item");
    expect(pluralize(2, "item")).toBe("2 items");
  });
});

describe("orders", () => {
  it("parses a batch of order numbers safely", () => {
    expect(parseOrderIds("123,456,123,abc,,7 8")).toEqual(["123", "456"]);
    expect(parseOrderIds(null)).toEqual([]);
  });

  it("summarizes order items", () => {
    const order = { items: [{ productName: "A" }, { productName: "B" }, { productName: "C" }] } as unknown as Order;
    expect(summarizeItems(order)).toBe("A and 2 more");
  });
});

describe("seller form", () => {
  it("converts prices to and from minor units", () => {
    expect(parsePriceToMinor("19.99")).toBe(1999);
    expect(parsePriceToMinor("5")).toBe(500);
    expect(parsePriceToMinor("1.234")).toBeUndefined();
    expect(parsePriceToMinor("-3")).toBeUndefined();
    expect(minorToInput(1999)).toBe("19.99");
  });

  it("validates required fields for a new product", () => {
    const errors = validateProductForm(EMPTY_FORM, "create");
    expect(Object.keys(errors).sort()).toEqual(["description", "name", "price"]);
  });

  it("accepts a complete product", () => {
    const values = { ...EMPTY_FORM, name: "Lamp", description: "Warm light", price: "29.00" };
    expect(validateProductForm(values, "create")).toEqual({});
    expect(validateProductForm(values, "edit")).toEqual({});
  });
});
