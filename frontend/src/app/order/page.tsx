"use client";

import Link from "next/link";
import { useSearchParams } from "next/navigation";
import { Suspense, useEffect, useState } from "react";
import { Breadcrumbs } from "@/shared/components/Breadcrumbs";
import { EmptyState, ErrorState, LoadingState, Spinner } from "@/shared/components/Feedback";
import { Icon } from "@/shared/components/Icon";
import { OrderStatusBadge } from "@/features/orders/components/OrderStatusBadge";
import { OrderTimeline } from "@/features/orders/components/OrderTimeline";
import { PaymentCountdown } from "@/features/orders/components/PaymentCountdown";
import { ProductArtwork } from "@/features/catalog/components/ProductArtwork";
import { useAsync } from "@/shared/hooks/useAsync";
import { useUser } from "@/shared/hooks/useUser";
import { api, isNotFound, toError } from "@/shared/lib/api";
import { formatDateTime, formatMoney, shortOrderNumber } from "@/shared/lib/format";
import type { InventoryReservation, OrderStatus } from "@/shared/lib/types";
import { toast } from "@/shared/lib/toast";

const BANNERS: Partial<Record<OrderStatus, { tone: string; title: string; detail: string }>> = {
  PAID: { tone: "border-emerald-200 bg-emerald-50 text-emerald-900", title: "Payment complete", detail: "Your order is confirmed and your stock is secured." },
  EXPIRED: { tone: "border-slate-200 bg-slate-100 text-slate-800", title: "This order expired", detail: "The payment window closed and the items were released back into stock." },
  REJECTED: { tone: "border-rose-200 bg-rose-50 text-rose-900", title: "Items unavailable", detail: "An item sold out before the reservation could be made. You were not charged." },
  FAILED: { tone: "border-rose-200 bg-rose-50 text-rose-900", title: "We couldn't complete this order", detail: "No payment was taken. You can try ordering again." },
  INVENTORY_CONFIRMATION_FAILED: {
    tone: "border-rose-200 bg-rose-50 text-rose-900",
    title: "Stock confirmation needs recovery",
    detail: "Payment was received, but inventory confirmation exhausted its retries. Operations can replay the dead-letter command.",
  },
};

function describeReservation(reservation: InventoryReservation): string {
  if (reservation.status === "CONFIRMED") return "Stock confirmed for this order";
  if (reservation.status === "CANCELLED") return "Stock released back to the shelf";
  return `Stock held until ${formatDateTime(reservation.expiresAt)}`;
}

/** Tracks one order through reservation, payment, and asynchronous confirmation. */
function OrderDetail({ orderId }: Readonly<{ orderId: string }>) {
  const { userId } = useUser();
  const [paying, setPaying] = useState(false);

  const order = useAsync(() => api.getOrder(orderId, userId), [orderId, userId]);
  const status = order.data?.status;
  const reservation = useAsync(
    () => (status ? api.getReservation(orderId).catch(() => null) : Promise.resolve(undefined)),
    [orderId, status],
  );

  const reload = order.reload;
  const processing = status === "ACCEPTED" || status === "CONFIRMING_INVENTORY";
  useEffect(() => {
    if (!processing) return;
    const timer = window.setInterval(reload, 1500);
    return () => window.clearInterval(timer);
  }, [processing, reload]);

  async function pay() {
    setPaying(true);
    try {
      await api.payOrder(orderId, userId, `payment-${orderId}`);
      toast("Payment received");
    } catch (reason) {
      toast(toError(reason, "Payment failed").message, "error");
    } finally {
      setPaying(false);
      reload();
    }
  }

  if (order.error && !order.data) {
    return isNotFound(order.error) ? (
      <EmptyState
        title="We couldn't find that order"
        detail={`There is no order ${shortOrderNumber(orderId)} in your order history.`}
        action={
          <Link href="/orders/" className="btn btn-primary">
            View your orders
          </Link>
        }
      />
    ) : (
      <ErrorState error={order.error} title="We couldn't load this order" retry={order.reload} />
    );
  }
  if (!order.data) return <LoadingState label="Finding your order" />;

  const data = order.data;
  const banner = BANNERS[data.status];

  return (
    <>
      <Breadcrumbs items={[{ label: "Home", href: "/" }, { label: "Orders", href: "/orders/" }, { label: `Order ${shortOrderNumber(data.orderId)}` }]} />

      <div className="flex flex-wrap items-start justify-between gap-4">
        <div>
          <p className="eyebrow">Order</p>
          <h1 className="mt-1 text-2xl font-bold tracking-tight sm:text-3xl">{data.orderId}</h1>
          <p className="mt-1 text-slate-500">Placed {formatDateTime(data.createdAt)}</p>
        </div>
        <OrderStatusBadge status={data.status} />
      </div>

      <div className="mt-8 grid items-start gap-8 lg:grid-cols-[1fr_22rem]">
        <div className="space-y-6">
          <section className="card p-6" aria-label="Order progress">
            <OrderTimeline status={data.status} />
            {data.status === "ACCEPTED" && (
              <p role="status" className="mt-6 flex items-center justify-center gap-2 text-sm text-sky-700">
                <Spinner className="size-4" /> Reserving your items. This page updates automatically.
              </p>
            )}
            {data.status === "CONFIRMING_INVENTORY" && (
              <p role="status" className="mt-6 flex items-center justify-center gap-2 text-sm text-sky-700">
                <Spinner className="size-4" /> Payment received. Confirming inventory asynchronously.
              </p>
            )}
          </section>

          {banner && (
            <div role="status" className={`rounded-2xl border p-5 ${banner.tone}`}>
              <p className="font-semibold">{banner.title}</p>
              <p className="text-sm">{banner.detail}</p>
            </div>
          )}

          <section className="card px-6" aria-labelledby="items-heading">
            <h2 id="items-heading" className="pt-6 text-lg font-bold">
              Items
            </h2>
            <ul className="divide-y divide-slate-100">
              {data.items.map((item) => (
                <li key={item.productId} className="flex items-center gap-4 py-5">
                  <ProductArtwork productId={item.productId} name={item.productName} className="size-16 shrink-0 rounded-xl" />
                  <div className="min-w-0 flex-1">
                    <p className="font-medium">{item.productName}</p>
                    <p className="text-sm text-slate-500 tabular-nums">
                      {item.quantity} × {formatMoney(item.unitPriceMinor, data.currency)}
                    </p>
                  </div>
                  <p className="font-semibold tabular-nums">{formatMoney(item.unitPriceMinor * item.quantity, data.currency)}</p>
                </li>
              ))}
            </ul>
          </section>
        </div>

        <aside aria-label="Order summary" className="card space-y-5 p-6 lg:sticky lg:top-40">
          <h2 className="text-lg font-bold">Summary</h2>
          <dl className="space-y-3 text-sm">
            <div className="flex justify-between">
              <dt className="text-slate-500">Status</dt>
              <dd>
                <OrderStatusBadge status={data.status} />
              </dd>
            </div>
            <div className="flex justify-between">
              <dt className="text-slate-500">Placed</dt>
              <dd className="text-right">{formatDateTime(data.createdAt)}</dd>
            </div>
            <div className="flex justify-between">
              <dt className="text-slate-500">Pay by</dt>
              <dd className="text-right">{formatDateTime(data.paymentDeadline)}</dd>
            </div>
            {reservation.data && (
              <div className="flex justify-between gap-4">
                <dt className="text-slate-500">Inventory</dt>
                <dd className="text-right">{describeReservation(reservation.data)}</dd>
              </div>
            )}
            <div className="flex justify-between border-t border-slate-100 pt-3 text-base font-bold">
              <dt>Total</dt>
              <dd className="tabular-nums">{formatMoney(data.totalMinor, data.currency)}</dd>
            </div>
          </dl>

          {data.status === "PENDING_PAYMENT" && (
            <div className="space-y-3">
              <p className="text-center">
                <PaymentCountdown deadline={data.paymentDeadline} />
              </p>
              <button type="button" className="btn btn-primary w-full" disabled={paying} onClick={() => void pay()}>
                {paying ? (
                  <>
                    <Spinner className="size-4" /> Processing payment…
                  </>
                ) : (
                  `Pay ${formatMoney(data.totalMinor, data.currency)}`
                )}
              </button>
              <p className="text-center text-xs text-slate-500">Simulated payment. No card is charged.</p>
            </div>
          )}

          <Link href="/orders/" className="btn btn-secondary w-full">
            <Icon name="chevron-left" className="size-4" /> All orders
          </Link>
        </aside>
      </div>
    </>
  );
}

function OrderRoute() {
  const params = useSearchParams();
  const orderId = params.get("id") ?? "";
  if (!/^\d{1,19}$/.test(orderId)) {
    return (
      <EmptyState
        title="This order link isn't valid"
        detail="Open the order from your order history instead."
        action={
          <Link href="/orders/" className="btn btn-primary">
            View your orders
          </Link>
        }
      />
    );
  }
  return <OrderDetail key={orderId} orderId={orderId} />;
}

export default function OrderPage() {
  return (
    <div className="container-page py-8">
      <Suspense fallback={<LoadingState label="Finding your order" />}>
        <OrderRoute />
      </Suspense>
    </div>
  );
}
