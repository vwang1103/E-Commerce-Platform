import type { OrderStatus } from "@/shared/lib/types";

const PRESENTATION: Record<OrderStatus, { label: string; tone: string }> = {
  ACCEPTED: { label: "Processing", tone: "bg-sky-50 text-sky-700 ring-sky-200" },
  PENDING_PAYMENT: { label: "Awaiting payment", tone: "bg-amber-50 text-amber-800 ring-amber-200" },
  CONFIRMING_INVENTORY: { label: "Confirming stock", tone: "bg-sky-50 text-sky-700 ring-sky-200" },
  INVENTORY_CONFIRMATION_FAILED: { label: "Needs recovery", tone: "bg-rose-50 text-rose-700 ring-rose-200" },
  PAID: { label: "Paid", tone: "bg-emerald-50 text-emerald-700 ring-emerald-200" },
  EXPIRED: { label: "Expired", tone: "bg-slate-100 text-slate-600 ring-slate-200" },
  REJECTED: { label: "Unavailable", tone: "bg-rose-50 text-rose-700 ring-rose-200" },
  FAILED: { label: "Failed", tone: "bg-rose-50 text-rose-700 ring-rose-200" },
};

export function orderStatusLabel(status: OrderStatus): string {
  return PRESENTATION[status].label;
}

export function OrderStatusBadge({ status }: Readonly<{ status: OrderStatus }>) {
  const { label, tone } = PRESENTATION[status];
  return (
    <span
      className={`status status-${status.toLowerCase().replace("_", "-")} inline-flex items-center gap-1.5 rounded-full px-3 py-1 text-xs font-semibold ring-1 ring-inset ${tone}`}
    >
      <span aria-hidden="true" className="size-1.5 rounded-full bg-current" />
      {label}
    </span>
  );
}
