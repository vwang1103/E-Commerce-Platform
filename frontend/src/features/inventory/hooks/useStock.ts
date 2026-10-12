"use client";

import { useAsync } from "@/shared/hooks/useAsync";
import { loadStock } from "@/features/inventory/lib/stock";

/** Available units by product ID, or undefined until the first answer arrives. */
export function useStock(productIds: readonly number[]): ReadonlyMap<number, number> | undefined {
  const key = productIds.join("|");
  return useAsync(
    () => (productIds.length > 0 ? loadStock(productIds) : Promise.resolve(new Map<number, number>())),
    [key],
  ).data;
}
