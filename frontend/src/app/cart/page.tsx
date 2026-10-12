"use client";

import Link from "next/link";
import { useMemo } from "react";
import { Breadcrumbs } from "@/shared/components/Breadcrumbs";
import { CartLineRow } from "@/features/cart/components/CartLineRow";
import { EmptyState, LoadingState } from "@/shared/components/Feedback";
import { Icon } from "@/shared/components/Icon";
import { TotalsText } from "@/features/cart/components/Totals";
import { useCart } from "@/features/cart/hooks/useCart";
import { useCartChecks } from "@/features/cart/hooks/useCartChecks";
import { cartTotals } from "@/features/cart/lib/cart";
import { pluralize } from "@/shared/lib/format";
import { useHydrated } from "@/shared/lib/store";

export default function CartPage() {
  const hydrated = useHydrated();
  const { lines, count, setQuantity, remove, clear } = useCart();
  const { checked } = useCartChecks(lines);
  const totals = useMemo(
    () => cartTotals(checked.map((entry) => ({ unitPriceMinor: entry.unitPriceMinor, currency: entry.currency, quantity: entry.line.quantity }))),
    [checked],
  );
  const blocked = checked.filter((entry) => entry.issue).length;

  return (
    <div className="container-page py-8">
      <Breadcrumbs items={[{ label: "Home", href: "/" }, { label: "Cart" }]} />
      <h1 className="page-title">Your cart</h1>

      {!hydrated ? (
        <LoadingState label="Loading your cart" />
      ) : lines.length === 0 ? (
        <div className="mt-8">
          <EmptyState
            icon="cart"
            title="Your cart is empty"
            detail="Add a few things you like and they will wait for you here."
            action={
              <Link href="/shop/" className="btn btn-primary">
                Start shopping
              </Link>
            }
          />
        </div>
      ) : (
        <div className="mt-8 grid items-start gap-8 lg:grid-cols-[1fr_22rem]">
          <section aria-label="Cart items" className="card px-5 sm:px-6">
            <ul className="divide-y divide-slate-100">
              {checked.map((entry) => (
                <CartLineRow
                  key={entry.line.productId}
                  checked={entry}
                  onQuantityChange={(quantity) => setQuantity(entry.line.productId, quantity)}
                  onRemove={() => remove([entry.line.productId])}
                />
              ))}
            </ul>
            <div className="border-t border-slate-100 py-4">
              <button type="button" className="text-sm font-medium text-slate-500 hover:text-rose-600" onClick={clear}>
                Empty cart
              </button>
            </div>
          </section>

          <aside aria-label="Order summary" className="card space-y-4 p-6 lg:sticky lg:top-40">
            <h2 className="text-lg font-bold">Order summary</h2>
            <dl className="space-y-2 text-sm">
              <div className="flex justify-between">
                <dt className="text-slate-500">{pluralize(count, "item")}</dt>
                <dd className="tabular-nums">
                  <TotalsText totals={totals} />
                </dd>
              </div>
              <div className="flex justify-between border-t border-slate-100 pt-3 text-base font-bold">
                <dt>Total</dt>
                <dd className="tabular-nums">
                  <TotalsText totals={totals} />
                </dd>
              </div>
            </dl>
            {blocked > 0 && (
              <p role="status" className="rounded-xl bg-rose-50 p-3 text-sm text-rose-800">
                {pluralize(blocked, "item")} need attention before you can check out.
              </p>
            )}
            <Link href="/checkout/" className="btn btn-primary w-full">
              Continue to checkout <Icon name="arrow-right" className="size-4" />
            </Link>
            <Link href="/shop/" className="btn btn-secondary w-full">
              Keep shopping
            </Link>
            <p className="text-xs text-slate-500">
              Each item becomes its own order so stock is reserved independently. You pay for them together after checkout.
            </p>
          </aside>
        </div>
      )}
    </div>
  );
}
