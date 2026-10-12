import type { ProductSummary } from "@/shared/lib/types";

export type SortKey = "featured" | "newest" | "price-asc" | "price-desc" | "name";

export const SORT_OPTIONS: ReadonlyArray<{ value: SortKey; label: string }> = [
  { value: "featured", label: "Featured" },
  { value: "newest", label: "Newest" },
  { value: "price-asc", label: "Price: low to high" },
  { value: "price-desc", label: "Price: high to low" },
  { value: "name", label: "Name: A to Z" },
];

export function isSortKey(value: string | null | undefined): value is SortKey {
  return SORT_OPTIONS.some((option) => option.value === value);
}

export interface PriceBucket {
  id: string;
  label: string;
  minMinor: number;
  /** Exclusive upper bound. */
  maxMinor?: number;
}

export const PRICE_BUCKETS: readonly PriceBucket[] = [
  { id: "under-25", label: "Under $25", minMinor: 0, maxMinor: 2500 },
  { id: "25-50", label: "$25 to $50", minMinor: 2500, maxMinor: 5000 },
  { id: "50-100", label: "$50 to $100", minMinor: 5000, maxMinor: 10000 },
  { id: "100-plus", label: "$100 and up", minMinor: 10000 },
];

export function bucketById(id: string | null | undefined): PriceBucket | undefined {
  return PRICE_BUCKETS.find((bucket) => bucket.id === id);
}

type Browsable = Pick<ProductSummary, "productId" | "name" | "priceMinor" | "categoryId">;

export interface BrowseFilters {
  categoryId?: number;
  bucket?: PriceBucket;
}

export function filterProducts<T extends Browsable>(items: readonly T[], filters: BrowseFilters): T[] {
  const { categoryId, bucket } = filters;
  return items.filter((item) => {
    if (categoryId !== undefined && item.categoryId !== categoryId) return false;
    if (bucket && (item.priceMinor < bucket.minMinor || (bucket.maxMinor !== undefined && item.priceMinor >= bucket.maxMinor))) {
      return false;
    }
    return true;
  });
}

export function sortProducts<T extends Browsable>(items: readonly T[], sort: SortKey): T[] {
  const copy = [...items];
  switch (sort) {
    case "newest":
      return copy.sort((a, b) => b.productId - a.productId);
    case "price-asc":
      return copy.sort((a, b) => a.priceMinor - b.priceMinor || a.productId - b.productId);
    case "price-desc":
      return copy.sort((a, b) => b.priceMinor - a.priceMinor || a.productId - b.productId);
    case "name":
      return copy.sort((a, b) => a.name.localeCompare(b.name));
    default:
      return copy;
  }
}

export function countByCategory<T extends Pick<ProductSummary, "categoryId">>(items: readonly T[]): Map<number, number> {
  const counts = new Map<number, number>();
  for (const item of items) counts.set(item.categoryId, (counts.get(item.categoryId) ?? 0) + 1);
  return counts;
}

/** Page numbers to render, with `null` marking a gap. Pages are zero-based. */
export function pageWindow(current: number, total: number): Array<number | null> {
  if (total <= 7) return Array.from({ length: total }, (_, index) => index);
  const pages = new Set([0, total - 1, current - 1, current, current + 1]);
  const sorted = [...pages].filter((page) => page >= 0 && page < total).sort((a, b) => a - b);
  const result: Array<number | null> = [];
  sorted.forEach((page, index) => {
    if (index > 0 && page - sorted[index - 1] > 1) result.push(null);
    result.push(page);
  });
  return result;
}
