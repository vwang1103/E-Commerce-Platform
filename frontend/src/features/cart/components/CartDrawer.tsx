"use client";

import Link from "next/link";
import { useEffect, useRef } from "react";
import { cartActions, useCart } from "@/features/cart/hooks/useCart";
import { useModalBehavior } from "@/shared/hooks/useModalBehavior";
import { useCategories } from "@/features/catalog/hooks/useCategories";
import { MAX_LINE_QUANTITY } from "@/features/cart/lib/cart";
import { formatMoney, pluralize } from "@/shared/lib/format";
import { ProductArtwork } from "@/features/catalog/components/ProductArtwork";
import { Icon } from "@/shared/components/Icon";
import { QuantityStepper } from "@/shared/components/QuantityStepper";
import { TotalsText } from "./Totals";

const close = () => cartActions.setDrawerOpen(false);

export function CartDrawer() {
  const { lines, count, totals, drawerOpen, setQuantity, remove } = useCart();
  const { byId } = useCategories();
  const closeButton = useRef<HTMLButtonElement>(null);
  useModalBehavior(drawerOpen, close);

  useEffect(() => {
    if (drawerOpen) closeButton.current?.focus();
  }, [drawerOpen]);

  if (!drawerOpen) return null;

  return (
    <div className="fixed inset-0 z-50">
      <button type="button" tabIndex={-1} aria-label="Close cart" className="absolute inset-0 animate-fade-in bg-slate-900/50" onClick={close} />
      <aside
        role="dialog"
        aria-modal="true"
        aria-label="Shopping cart"
        className="absolute inset-y-0 right-0 flex w-full max-w-md animate-slide-in-right flex-col bg-white shadow-2xl"
      >
        <header className="flex items-center justify-between border-b border-slate-200 px-5 py-4">
          <h2 className="text-lg font-bold">
            Your cart <span className="font-normal text-slate-500">({pluralize(count, "item")})</span>
          </h2>
          <button ref={closeButton} type="button" className="btn btn-ghost size-9 rounded-full p-0" onClick={close} aria-label="Close cart">
            <Icon name="close" />
          </button>
        </header>

        {lines.length === 0 ? (
          <div className="flex flex-1 flex-col items-center justify-center gap-4 px-6 text-center">
            <span className="flex size-16 items-center justify-center rounded-full bg-emerald-50 text-emerald-600">
              <Icon name="cart" className="size-8" />
            </span>
            <p className="font-semibold">Your cart is empty</p>
            <Link href="/shop/" onClick={close} className="btn btn-primary">
              Start shopping
            </Link>
          </div>
        ) : (
          <>
            <ul className="flex-1 divide-y divide-slate-100 overflow-y-auto px-5">
              {lines.map((line) => (
                <li key={line.productId} className="flex gap-4 py-4">
                  <Link href={`/product/?id=${line.productId}`} onClick={close} className="shrink-0" tabIndex={-1} aria-hidden="true">
                    <ProductArtwork productId={line.productId} name={line.name} categoryId={line.categoryId} className="size-20 rounded-xl" />
                  </Link>
                  <div className="flex min-w-0 flex-1 flex-col">
                    <div className="flex items-start justify-between gap-2">
                      <div className="min-w-0">
                        <Link href={`/product/?id=${line.productId}`} onClick={close} className="line-clamp-2 font-medium hover:text-emerald-700">
                          {line.name}
                        </Link>
                        <p className="text-xs text-slate-500">{byId(line.categoryId).name}</p>
                      </div>
                      <p className="font-semibold tabular-nums">{formatMoney(line.unitPriceMinor * line.quantity, line.currency)}</p>
                    </div>
                    <div className="mt-auto flex items-center justify-between pt-2">
                      <QuantityStepper
                        size="sm"
                        label={`Quantity of ${line.name}`}
                        value={line.quantity}
                        max={MAX_LINE_QUANTITY}
                        onChange={(quantity) => setQuantity(line.productId, quantity)}
                      />
                      <button
                        type="button"
                        className="flex items-center gap-1 text-xs font-medium text-slate-500 hover:text-rose-600"
                        onClick={() => remove([line.productId])}
                        aria-label={`Remove ${line.name}`}
                      >
                        <Icon name="trash" className="size-4" /> Remove
                      </button>
                    </div>
                  </div>
                </li>
              ))}
            </ul>
            <footer className="space-y-3 border-t border-slate-200 bg-slate-50 px-5 py-4">
              <div className="flex items-center justify-between text-base">
                <span className="font-medium">Subtotal</span>
                <span className="text-lg font-bold tabular-nums">
                  <TotalsText totals={totals} />
                </span>
              </div>
              <p className="text-xs text-slate-500">Stock is reserved when you check out. You then have 15 minutes to pay.</p>
              <Link href="/checkout/" onClick={close} className="btn btn-primary w-full">
                Checkout
              </Link>
              <Link href="/cart/" onClick={close} className="btn btn-secondary w-full">
                View full cart
              </Link>
            </footer>
          </>
        )}
      </aside>
    </div>
  );
}
