"use client";

import Link from "next/link";
import { useRouter, useSearchParams } from "next/navigation";
import { Suspense, useEffect, useMemo, useState } from "react";
import { EmptyState, ErrorState, LoadingState, Spinner } from "@/shared/components/Feedback";
import { Icon } from "@/shared/components/Icon";
import { OrderStatusBadge } from "@/features/orders/components/OrderStatusBadge";
import { Pagination } from "@/shared/components/Pagination";
import { PaymentCountdown } from "@/features/orders/components/PaymentCountdown";
import { ProductArtwork } from "@/features/catalog/components/ProductArtwork";
import { useAsync } from "@/shared/hooks/useAsync";
import { useUser } from "@/shared/hooks/useUser";
import { api, toError } from "@/shared/lib/api";
import { formatDateTime, formatMoney, pluralize, shortOrderNumber } from "@/shared/lib/format";
import { ORDER_TABS, isOrderTab, matchesTab, summarizeItems } from "@/features/orders/lib/orders";
import { toast } from "@/shared/lib/toast";

const PAGE_SIZE = 10;
const FETCH_LIMIT = 100;

/** Lists the current user's orders and refreshes while workflows remain active. */
function Orders() {
  const params = useSearchParams();
  const router = useRouter();
  const { userId, username } = useUser();
  const [payingId, setPayingId] = useState<string>();

  const tabParam = params.get("tab");
  const tab = isOrderTab(tabParam) ? tabParam : "all";
  const requestedPage = Math.max(1, Math.floor(Number(params.get("page"))) || 1) - 1;

  const orders = useAsync(() => api.listOrders(userId, 0, FETCH_LIMIT), [userId]);
  const all = useMemo(() => orders.data?.items ?? [], [orders.data]);
  const visible = useMemo(() => all.filter((order) => matchesTab(order, tab)), [all, tab]);
  const totalPages = Math.max(1, Math.ceil(visible.length / PAGE_SIZE));
  const page = Math.min(requestedPage, totalPages - 1);
  const pageItems = visible.slice(page * PAGE_SIZE, (page + 1) * PAGE_SIZE);

  const processing = all.some((order) => order.status === "ACCEPTED");
  const reload = orders.reload;
  useEffect(() => {
    if (!processing) return;
    const timer = window.setInterval(reload, 2000);
    return () => window.clearInterval(timer);
  }, [processing, reload]);

  function navigate(next: { tab?: string; page?: number }) {
    const query = new URLSearchParams();
    const nextTab = next.tab ?? tab;
    if (nextTab !== "all") query.set("tab", nextTab);
    if (next.page) query.set("page", String(next.page + 1));
    const text = query.toString();
    router.replace(text ? `/orders/?${text}` : "/orders/", { scroll: false });
  }

  async function pay(orderId: string) {
    setPayingId(orderId);
    try {
      await api.payOrder(orderId, userId, `payment-${orderId}`);
      toast("Payment received");
    } catch (reason) {
      toast(toError(reason, "Payment failed").message, "error");
    } finally {
      setPayingId(undefined);
      reload();
    }
  }

  return (
    <div className="container-page py-10">
      <div className="flex flex-wrap items-end justify-between gap-3">
        <div>
          <p className="eyebrow">{username}</p>
          <h1 className="page-title mt-1">Your orders</h1>
        </div>
        <Link href="/shop/" className="btn btn-secondary btn-sm">
          Continue shopping
        </Link>
      </div>

      <div role="tablist" aria-label="Filter orders" className="mt-8 flex flex-wrap gap-2">
        {ORDER_TABS.map((entry) => (
          <button
            key={entry.id}
            type="button"
            role="tab"
            aria-selected={tab === entry.id}
            aria-current={tab === entry.id ? "true" : undefined}
            className="chip"
            onClick={() => navigate({ tab: entry.id })}
          >
            {entry.label}
            {orders.data && <span className="text-xs opacity-70">{all.filter((order) => matchesTab(order, entry.id)).length}</span>}
          </button>
        ))}
      </div>

      <div className="mt-6">
        {orders.error && !orders.data ? (
          <ErrorState error={orders.error} title="We couldn't load your orders" retry={orders.reload} />
        ) : !orders.data ? (
          <LoadingState label="Loading your orders" />
        ) : visible.length === 0 ? (
          <EmptyState
            title={all.length === 0 ? "No orders yet" : "No orders in this view"}
            detail={all.length === 0 ? "When you check out, your orders will show up here." : "Try a different filter."}
            action={
              <Link href="/shop/" className="btn btn-primary">
                Browse products
              </Link>
            }
          />
        ) : (
          <>
            <ul className="space-y-4">
              {pageItems.map((order) => (
                <li key={order.orderId} className="card p-5">
                  <div className="flex flex-wrap items-center justify-between gap-3 text-sm text-slate-500">
                    <p>
                      Order <span className="font-medium text-slate-900">{shortOrderNumber(order.orderId)}</span> · {formatDateTime(order.createdAt)}
                    </p>
                    <OrderStatusBadge status={order.status} />
                  </div>
                  <div className="mt-4 flex flex-wrap items-center gap-4">
                    <div className="flex -space-x-3">
                      {order.items.slice(0, 3).map((item) => (
                        <ProductArtwork key={item.productId} productId={item.productId} name={item.productName} className="size-14 rounded-xl ring-2 ring-white" />
                      ))}
                    </div>
                    <div className="min-w-0 flex-1">
                      <Link href={`/order/?id=${order.orderId}`} className="font-semibold hover:text-emerald-700">
                        {summarizeItems(order)}
                      </Link>
                      <p className="text-sm text-slate-500">
                        {pluralize(
                          order.items.reduce((sum, item) => sum + item.quantity, 0),
                          "unit",
                        )}
                      </p>
                    </div>
                    <p className="text-lg font-bold tabular-nums">{formatMoney(order.totalMinor, order.currency)}</p>
                  </div>
                  <div className="mt-4 flex flex-wrap items-center justify-between gap-3 border-t border-slate-100 pt-4">
                    {order.status === "PENDING_PAYMENT" ? (
                      <PaymentCountdown deadline={order.paymentDeadline} />
                    ) : order.status === "ACCEPTED" ? (
                      <span className="flex items-center gap-2 text-sm text-sky-700">
                        <Spinner className="size-4" /> Reserving your items…
                      </span>
                    ) : (
                      <span />
                    )}
                    <div className="flex gap-2">
                      {order.status === "PENDING_PAYMENT" && (
                        <button
                          type="button"
                          className="btn btn-primary btn-sm"
                          disabled={payingId === order.orderId}
                          onClick={() => void pay(order.orderId)}
                        >
                          {payingId === order.orderId ? "Paying…" : "Pay now"}
                        </button>
                      )}
                      <Link href={`/order/?id=${order.orderId}`} className="btn btn-secondary btn-sm">
                        Details <Icon name="chevron-right" className="size-4" />
                      </Link>
                    </div>
                  </div>
                </li>
              ))}
            </ul>
            <Pagination page={page} totalPages={totalPages} onChange={(next) => navigate({ page: next })} />
            {orders.data.totalElements > FETCH_LIMIT && (
              <p className="mt-6 text-center text-sm text-slate-500">Showing your {FETCH_LIMIT} most recent orders.</p>
            )}
          </>
        )}
      </div>
    </div>
  );
}

export default function OrdersPage() {
  return (
    <Suspense
      fallback={
        <div className="container-page py-10">
          <LoadingState label="Loading your orders" />
        </div>
      }
    >
      <Orders />
    </Suspense>
  );
}
