"use client";

import { useMemo } from "react";
import { useAsync } from "@/shared/hooks/useAsync";
import { ApiError, api } from "@/shared/lib/api";
import { checkLine, type CartLine, type CheckedLine, type LineCheck } from "@/features/cart/lib/cart";

async function lookup<T>(load: () => Promise<T>): Promise<T | null | undefined> {
  try {
    return await load();
  } catch (reason) {
    // A confirmed 404 is an answer; any other failure leaves the line unchecked.
    return reason instanceof ApiError && reason.status === 404 ? null : undefined;
  }
}

/**
 * Compares cart lines with the live catalog and stock so the cart can warn about
 * price changes, unavailable products, and quantities that exceed available stock.
 */
export function useCartChecks(lines: readonly CartLine[]) {
  const signature = lines.map((line) => line.productId).join("|");

  const checks = useAsync(async () => {
    const entries = await Promise.all(
      lines.map(async (line): Promise<[number, LineCheck]> => [
        line.productId,
        {
          product: await lookup(() => api.getProduct(line.productId)),
          inventory: await lookup(() => api.getInventory(line.productId)),
        },
      ]),
    );
    return new Map(entries);
  }, [signature]);

  const checked = useMemo<CheckedLine[]>(
    () => lines.map((line) => checkLine(line, checks.data?.get(line.productId))),
    [lines, checks.data],
  );

  return { checked, ready: !checks.loading || checks.data !== undefined, reload: checks.reload };
}
