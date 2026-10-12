import { api } from "@/shared/lib/api";
import type { Product } from "@/shared/lib/types";

const TTL_MS = 30_000;

let cached: { at: number; promise: Promise<Product[]> } | undefined;

/** Shares one catalog fetch between pages so client-side navigation stays instant. */
export function loadCatalog(): Promise<Product[]> {
  if (cached && Date.now() - cached.at < TTL_MS) return cached.promise;
  const promise = api.listAllProducts();
  const entry = { at: Date.now(), promise };
  cached = entry;
  promise.catch(() => {
    if (cached === entry) cached = undefined;
  });
  return promise;
}

export function invalidateCatalog(): void {
  cached = undefined;
}
