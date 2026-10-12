import { describe, expect, it } from "vitest";
import { PRICE_BUCKETS, bucketById, countByCategory, filterProducts, pageWindow, sortProducts } from "@/features/catalog/lib/browse";

const items = [
  { productId: 1, name: "Charger", priceMinor: 5900, categoryId: 100 },
  { productId: 2, name: "Bottle", priceMinor: 3400, categoryId: 200 },
  { productId: 3, name: "Notes", priceMinor: 1600, categoryId: 300 },
  { productId: 4, name: "Headphones", priceMinor: 14900, categoryId: 100 },
];

describe("browse helpers", () => {
  it("filters by category and price bucket", () => {
    expect(filterProducts(items, { categoryId: 100 }).map((item) => item.productId)).toEqual([1, 4]);
    expect(filterProducts(items, { bucket: bucketById("25-50") }).map((item) => item.productId)).toEqual([2]);
    expect(filterProducts(items, { bucket: PRICE_BUCKETS[3] }).map((item) => item.productId)).toEqual([4]);
  });

  it("treats the bucket upper bound as exclusive", () => {
    const edge = [{ productId: 9, name: "Edge", priceMinor: 2500, categoryId: 100 }];
    expect(filterProducts(edge, { bucket: bucketById("under-25") })).toHaveLength(0);
    expect(filterProducts(edge, { bucket: bucketById("25-50") })).toHaveLength(1);
  });

  it("sorts without mutating the input", () => {
    const before = items.map((item) => item.productId);
    expect(sortProducts(items, "price-asc").map((item) => item.productId)).toEqual([3, 2, 1, 4]);
    expect(sortProducts(items, "price-desc").map((item) => item.productId)).toEqual([4, 1, 2, 3]);
    expect(sortProducts(items, "newest").map((item) => item.productId)).toEqual([4, 3, 2, 1]);
    expect(sortProducts(items, "name").map((item) => item.name)).toEqual(["Bottle", "Charger", "Headphones", "Notes"]);
    expect(items.map((item) => item.productId)).toEqual(before);
  });

  it("counts products per category", () => {
    expect(countByCategory(items).get(100)).toBe(2);
  });

  it("builds a compact page window with gaps", () => {
    expect(pageWindow(0, 5)).toEqual([0, 1, 2, 3, 4]);
    expect(pageWindow(5, 12)).toEqual([0, null, 4, 5, 6, null, 11]);
    expect(pageWindow(0, 12)).toEqual([0, 1, null, 11]);
  });
});
