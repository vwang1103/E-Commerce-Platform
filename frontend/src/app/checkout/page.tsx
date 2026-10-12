"use client";

import Link from "next/link";
import { useRouter, useSearchParams } from "next/navigation";
import { Suspense, useEffect, useMemo, useRef, useState } from "react";
import { Breadcrumbs } from "@/shared/components/Breadcrumbs";
import { CartLineRow } from "@/features/cart/components/CartLineRow";
import { EmptyState, ErrorState, LoadingState, Spinner } from "@/shared/components/Feedback";
import { Icon } from "@/shared/components/Icon";
import { OrderStatusBadge } from "@/features/orders/components/OrderStatusBadge";
import { PaymentCountdown } from "@/features/orders/components/PaymentCountdown";
import { TotalsText } from "@/features/cart/components/Totals";
import { useAsync } from "@/shared/hooks/useAsync";
import { cartActions, useCart } from "@/features/cart/hooks/useCart";
import { useCartChecks } from "@/features/cart/hooks/useCartChecks";
import { useUser } from "@/shared/hooks/useUser";
import { ApiError, api, newIdempotencyKey, toError } from "@/shared/lib/api";
import { cartTotals, type CheckedLine } from "@/features/cart/lib/cart";
import { formatMoney, pluralize } from "@/shared/lib/format";
import { earliestDeadline, parseOrderIds, summarizeItems } from "@/features/orders/lib/orders";
import { createMemoryStore, useHydrated, useStore } from "@/shared/lib/store";
import { toast } from "@/shared/lib/toast";

type LineProgress = { state: "placing" | "placed" } | { state: "failed"; message: string };

interface Failure {
  name: string;
  message: string;
}

// Survives the switch from the review step to the confirmation step.
const failureStore = createMemoryStore<Readonly<Record<string, Failure>>>({});

function describeFailure(reason: unknown): string {
  if (reason instanceof ApiError && reason.status === 409) return `This item couldn't be reserved: ${reason.message}`;
  return toError(reason, "This item couldn't be ordered.").message;
}

/** Reviews the cart, validates current stock, and places each selected line as an order. */
function Review() {
  const router = useRouter();
  const hydrated = useHydrated();
  const { userId, username } = useUser();
  const { lines, setQuantity, remove } = useCart();
  const { checked, ready } = useCartChecks(lines);
  const [placing, setPlacing] = useState(false);
  // Rows come from this snapshot while placing, because each placed line leaves the cart immediately.
  const [snapshot, setSnapshot] = useState<readonly CheckedLine[]>();
  const [progress, setProgress] = useState<Readonly<Record<string, LineProgress>>>({});
  const idempotencyKeys = useRef(new Map<string, string>());

  const totals = useMemo(
    () => cartTotals(checked.map((entry) => ({ unitPriceMinor: entry.unitPriceMinor, currency: entry.currency, quantity: entry.line.quantity }))),
    [checked],
  );
  const hasIssue = checked.some((entry) => entry.issue);

  async function placeOrders() {
    const batch = checked;
    setPlacing(true);
    setSnapshot(batch);
    setProgress({});
    failureStore.set({});
    setProgress(Object.fromEntries(batch.map(({ line }) => [line.productId, { state: "placing" }])));
    const signature = batch
      .map(({ line, unitPriceMinor }) => `${line.productId}:${line.quantity}:${unitPriceMinor}`)
      .sort()
      .join("|");
    const key = idempotencyKeys.current.get(signature) ?? newIdempotencyKey("checkout");
    idempotencyKeys.current.set(signature, key);
    try {
      const order = await api.createOrder(
        { items: batch.map(({ line }) => ({ productId: line.productId, quantity: line.quantity })) },
        userId,
        key,
      );
      cartActions.remove(batch.map(({ line }) => line.productId));
      setProgress(Object.fromEntries(batch.map(({ line }) => [line.productId, { state: "placed" }])));
      router.replace(`/checkout/?orders=${order.orderId}`);
    } catch (reason) {
      const message = describeFailure(reason);
      const failures = Object.fromEntries(batch.map(({ line }) => [line.productId, { name: line.name, message }]));
      failureStore.set(failures);
      setProgress(Object.fromEntries(batch.map(({ line }) => [line.productId, { state: "failed", message }])));
      setPlacing(false);
      setSnapshot(undefined);
      toast("The order could not be placed. Review the messages below.", "error");
    }
  }

  if (!hydrated) return <LoadingState label="Loading checkout" />;
  if (lines.length === 0 && !snapshot) {
    return (
      <EmptyState
        icon="cart"
        title="Nothing to check out"
        detail="Your cart is empty."
        action={
          <Link href="/shop/" className="btn btn-primary">
            Browse products
          </Link>
        }
      />
    );
  }

  return (
    <div className="grid items-start gap-8 lg:grid-cols-[1fr_22rem]">
      <div className="space-y-6">
        <section className="card p-6" aria-labelledby="customer-heading">
          <h2 id="customer-heading" className="font-bold">
            1. Customer
          </h2>
          <p className="mt-2 text-slate-600">
            Ordering as <strong>{username}</strong>.
          </p>
        </section>

        <section className="card px-6 pt-6 pb-1" aria-labelledby="review-heading">
          <div className="flex items-center justify-between">
            <h2 id="review-heading" className="font-bold">
              2. Review items
            </h2>
            <Link href="/cart/" className="text-sm font-medium text-emerald-700 hover:underline">
              Edit cart
            </Link>
          </div>
          <ul className="divide-y divide-slate-100">
            {(snapshot ?? checked).map((entry) => {
              const state = progress[entry.line.productId];
              return (
                <CartLineRow
                  key={entry.line.productId}
                  checked={entry}
                  onQuantityChange={placing ? undefined : (quantity) => setQuantity(entry.line.productId, quantity)}
                  onRemove={placing ? undefined : () => remove([entry.line.productId])}
                  note={
                    state?.state === "placing" ? (
                      <p className="flex items-center gap-2 text-sm text-emerald-700">
                        <Spinner className="size-4" /> Reserving stock…
                      </p>
                    ) : state?.state === "placed" ? (
                      <p className="flex items-center gap-2 text-sm text-emerald-700">
                        <Icon name="check" className="size-4" /> Reserved
                      </p>
                    ) : state?.state === "failed" ? (
                      <p role="alert" className="text-sm text-rose-700">
                        {state.message}
                      </p>
                    ) : null
                  }
                />
              );
            })}
          </ul>
        </section>

        <section className="card p-6" aria-labelledby="payment-heading">
          <h2 id="payment-heading" className="font-bold">
            3. Payment
          </h2>
          <p className="mt-2 text-slate-600">
            We reserve your stock first, then you have <strong>15 minutes</strong> to pay. This is a demonstration store, so payment is simulated and no card is charged.
          </p>
        </section>
      </div>

      <aside aria-label="Order summary" className="card space-y-4 p-6 lg:sticky lg:top-40">
        <h2 className="text-lg font-bold">Summary</h2>
        <dl className="space-y-2 text-sm">
          <div className="flex justify-between">
            <dt className="text-slate-500">{pluralize(lines.length, "order")} to place</dt>
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
        {hasIssue && (
          <p role="status" className="rounded-xl bg-rose-50 p-3 text-sm text-rose-800">
            Resolve the highlighted items to continue.
          </p>
        )}
        <button type="button" className="btn btn-primary w-full" disabled={placing || !ready || hasIssue} onClick={() => void placeOrders()}>
          {placing ? (
            <>
              <Spinner className="size-4" /> Placing orders…
            </>
          ) : (
            "Place order"
          )}
        </button>
        <p className="text-xs text-slate-500">All selected items are reserved together in one order.</p>
      </aside>
    </div>
  );
}

function PlacedOrders({ ids }: Readonly<{ ids: readonly string[] }>) {
  const { userId } = useUser();
  const failures = useStore(failureStore);
  const { lines } = useCart();
  const [paying, setPaying] = useState(false);
  const [payError, setPayError] = useState<Error>();
  const key = ids.join(",");

  const orders = useAsync(() => Promise.all(ids.map((id) => api.getOrder(id, userId))), [key, userId]);
  const list = useMemo(() => orders.data ?? [], [orders.data]);
  const processing = list.some(
    (order) => order.status === "ACCEPTED" || order.status === "CONFIRMING_INVENTORY",
  );
  const pending = list.filter((order) => order.status === "PENDING_PAYMENT");
  const allPaid = list.length > 0 && list.every((order) => order.status === "PAID");
  const deadline = earliestDeadline(list);
  const pendingTotals = useMemo(
    () => cartTotals(pending.map((order) => ({ unitPriceMinor: order.totalMinor, currency: order.currency, quantity: 1 }))),
    [pending],
  );
  const failed = Object.values(failures);

  const reload = orders.reload;
  useEffect(() => {
    if (!processing) return;
    const timer = window.setInterval(reload, 1500);
    return () => window.clearInterval(timer);
  }, [processing, reload]);

  async function payAll() {
    setPaying(true);
    setPayError(undefined);
    try {
      for (const order of pending) {
        await api.payOrder(order.orderId, userId, `payment-${order.orderId}`);
      }
      toast(pending.length === 1 ? "Payment received" : "All payments received");
    } catch (reason) {
      setPayError(toError(reason, "Payment failed"));
    } finally {
      setPaying(false);
      reload();
    }
  }

  if (orders.error && !orders.data) {
    return <ErrorState error={orders.error} title="We couldn't load your orders" retry={orders.reload} />;
  }
  if (!orders.data) return <LoadingState label="Loading your orders" />;

  return (
    <div className="mx-auto max-w-3xl">
      <div className="card p-8 text-center">
        <span
          className="mx-auto flex size-14 items-center justify-center rounded-full bg-emerald-100 text-emerald-700"
        >
          {processing ? <Spinner className="size-6" /> : <Icon name="check" className="size-7" />}
        </span>
        <h1 className="mt-4 text-2xl font-bold tracking-tight sm:text-3xl">
          {allPaid ? "Payment complete. Thank you!" : processing ? "Reserving your items…" : pending.length > 0 ? "Your items are reserved" : "Order update"}
        </h1>
        <p className="mt-2 text-slate-600">
          {allPaid
            ? "Your orders are confirmed."
            : processing
              ? "This usually takes a moment. The page updates by itself."
              : pending.length > 0
                ? "Complete payment before the timer runs out or the stock is released."
                : "Review the status of each order below."}
        </p>
        {deadline && (
          <p className="mt-3">
            <PaymentCountdown deadline={deadline} />
          </p>
        )}
        {pending.length > 0 && (
          <div className="mt-6">
            <button type="button" className="btn btn-primary min-w-56" disabled={paying} onClick={() => void payAll()}>
              {paying ? (
                <>
                  <Spinner className="size-4" /> Processing payment…
                </>
              ) : (
                <>
                  Pay <TotalsText totals={pendingTotals} />
                </>
              )}
            </button>
            <p className="mt-2 text-xs text-slate-500">Simulated payment. No card is charged.</p>
          </div>
        )}
        {payError && (
          <div className="mt-4 text-left">
            <ErrorState error={payError} title="Payment didn't go through" />
          </div>
        )}
      </div>

      {failed.length > 0 && (
        <div role="alert" className="mt-6 rounded-2xl border border-amber-200 bg-amber-50 p-5 text-amber-900">
          <p className="font-semibold">{pluralize(failed.length, "item")} couldn&apos;t be ordered</p>
          <ul className="mt-2 list-disc space-y-1 pl-5 text-sm">
            {failed.map((failure) => (
              <li key={failure.name}>
                <strong>{failure.name}:</strong> {failure.message}
              </li>
            ))}
          </ul>
          {lines.length > 0 && (
            <Link href="/cart/" className="btn btn-secondary btn-sm mt-3">
              Back to cart
            </Link>
          )}
        </div>
      )}

      <ul className="mt-6 space-y-4">
        {list.map((order) => (
          <li key={order.orderId} className="card flex flex-wrap items-center justify-between gap-4 p-5">
            <div className="min-w-0">
              <p className="truncate font-semibold">{summarizeItems(order)}</p>
              <p className="text-sm text-slate-500">
                {pluralize(
                  order.items.reduce((sum, item) => sum + item.quantity, 0),
                  "unit",
                )}{" "}
                · {formatMoney(order.totalMinor, order.currency)}
              </p>
            </div>
            <div className="flex items-center gap-3">
              <OrderStatusBadge status={order.status} />
              <Link href={`/order/?id=${order.orderId}`} className="btn btn-secondary btn-sm">
                Details
              </Link>
            </div>
          </li>
        ))}
      </ul>

      <div className="mt-8 flex flex-wrap justify-center gap-3">
        <Link href="/orders/" className="btn btn-secondary">
          All orders
        </Link>
        <Link href="/shop/" className="btn btn-ghost">
          Continue shopping
        </Link>
      </div>
    </div>
  );
}

function CheckoutRoute() {
  const params = useSearchParams();
  const ids = parseOrderIds(params.get("orders"));
  return ids.length > 0 ? <PlacedOrders key={ids.join(",")} ids={ids} /> : <Review />;
}

export default function CheckoutPage() {
  return (
    <div className="container-page py-8">
      <Breadcrumbs items={[{ label: "Home", href: "/" }, { label: "Cart", href: "/cart/" }, { label: "Checkout" }]} />
      <Suspense fallback={<LoadingState label="Loading checkout" />}>
        <CheckoutRoute />
      </Suspense>
    </div>
  );
}
