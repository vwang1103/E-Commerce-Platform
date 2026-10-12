import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import { api } from "@/shared/lib/api";
import { findCategoryById, findCategoryBySlug, toCategory } from "@/features/catalog/lib/categories";
import { invalidateStock, loadStock } from "@/features/inventory/lib/stock";
import type { Inventory } from "@/shared/lib/types";

const stock = (productId: number, availableQuantity: number): Inventory => ({
  productId,
  totalQuantity: 100,
  availableQuantity,
  reservedQuantity: 0,
  soldQuantity: 0,
  version: 0,
});

describe("loadStock", () => {
  beforeEach(() => invalidateStock());
  afterEach(() => vi.restoreAllMocks());

  it("maps products without a stock record to zero", async () => {
    vi.spyOn(api, "listInventory").mockResolvedValue([stock(1, 7)]);
    const result = await loadStock([1, 2]);
    expect(result.get(1)).toBe(7);
    expect(result.get(2)).toBe(0);
  });

  it("reuses fresh answers and only asks for product IDs it has not seen", async () => {
    const spy = vi.spyOn(api, "listInventory").mockResolvedValueOnce([stock(1, 3)]).mockResolvedValueOnce([stock(2, 9)]);
    await loadStock([1]);
    const result = await loadStock([1, 2]);
    expect(spy).toHaveBeenCalledTimes(2);
    expect(spy.mock.calls[1][0]).toEqual([2]);
    expect([...result]).toEqual([[1, 3], [2, 9]]);
  });

  it("splits large lookups into chunks of 100", async () => {
    const spy = vi.spyOn(api, "listInventory").mockResolvedValue([]);
    await loadStock(Array.from({ length: 150 }, (_, index) => index + 1));
    expect(spy.mock.calls.map((call) => call[0].length)).toEqual([100, 50]);
  });
});

describe("categories", () => {
  it("builds display categories from the API shape", () => {
    const category = toCategory({ categoryId: 400, parentCategoryId: null, name: "Fitness and Outdoors", slug: "fitness-and-outdoors" });
    expect(category).toMatchObject({ id: 400, slug: "fitness-and-outdoors" });
    expect(category.blurb).toMatch(/training/i);
  });

  it("falls back to a generic category for unknown IDs and slugs", () => {
    const categories = [toCategory({ categoryId: 100, parentCategoryId: null, name: "Electronics", slug: "electronics" })];
    expect(findCategoryById(categories, 999).name).toBe("General");
    expect(findCategoryBySlug(categories, "nope")).toBeUndefined();
    expect(findCategoryBySlug(categories, "electronics")?.id).toBe(100);
  });
});
