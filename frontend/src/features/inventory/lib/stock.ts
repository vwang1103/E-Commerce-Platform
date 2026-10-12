import { api } from "@/shared/lib/api";
import type { Inventory } from "@/shared/lib/types";

const TTL_MS = 10_000;
const CHUNK = 100;

const cache = new Map<number, { at: number; value: Inventory | null }>();

/**
 * Available units per product. A product with no stock record maps to 0, because the order
 * service cannot reserve it. Fresh entries are reused so paging does not refetch.
 */
export async function loadStock(productIds: readonly number[]): Promise<Map<number, number>> {
  const now = Date.now();
  const wanted = [...new Set(productIds)];
  const missing = wanted.filter((productId) => {
    const hit = cache.get(productId);
    return !hit || now - hit.at >= TTL_MS;
  });

  for (let start = 0; start < missing.length; start += CHUNK) {
    const chunk = missing.slice(start, start + CHUNK);
    const found = new Map((await api.listInventory(chunk)).map((entry) => [entry.productId, entry]));
    for (const productId of chunk) {
      cache.set(productId, { at: Date.now(), value: found.get(productId) ?? null });
    }
  }

  return new Map(
    wanted.map((productId) => [productId, Math.max(0, cache.get(productId)?.value?.availableQuantity ?? 0)]),
  );
}

export function invalidateStock(): void {
  cache.clear();
}
