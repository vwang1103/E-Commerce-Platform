"use client";

import { useMemo } from "react";
import {
  addLine,
  cartCount,
  cartTotals,
  EMPTY_CART,
  parseCart,
  removeLines,
  setLineQuantity,
  type CartLine,
} from "@/features/cart/lib/cart";
import { createMemoryStore, createPersistedStore, useStore } from "@/shared/lib/store";

const cartStore = createPersistedStore<readonly CartLine[]>("arcade.cart.v1", EMPTY_CART, parseCart);
const drawerStore = createMemoryStore(false);

export const cartActions = {
  add(line: CartLine) {
    cartStore.set(addLine(cartStore.getSnapshot(), line));
  },
  setQuantity(productId: number, quantity: number) {
    cartStore.set(setLineQuantity(cartStore.getSnapshot(), productId, quantity));
  },
  remove(productIds: readonly number[]) {
    cartStore.set(removeLines(cartStore.getSnapshot(), productIds));
  },
  clear() {
    cartStore.set(EMPTY_CART);
  },
  setDrawerOpen(open: boolean) {
    drawerStore.set(open);
  },
};

export function getCartLines(): readonly CartLine[] {
  return cartStore.getSnapshot();
}

export function useCart() {
  const lines = useStore(cartStore);
  const drawerOpen = useStore(drawerStore);
  const count = useMemo(() => cartCount(lines), [lines]);
  const totals = useMemo(() => cartTotals(lines), [lines]);
  return { lines, count, totals, drawerOpen, ...cartActions };
}
