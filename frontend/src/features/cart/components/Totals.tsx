import { formatMoney } from "@/shared/lib/format";
import type { CurrencyTotal } from "@/features/cart/lib/cart";

/** Renders per-currency totals, joined when a cart mixes currencies. */
export function TotalsText({ totals }: Readonly<{ totals: readonly CurrencyTotal[] }>) {
  if (totals.length === 0) return <>{formatMoney(0, "USD")}</>;
  return <>{totals.map((total) => formatMoney(total.minor, total.currency)).join(" + ")}</>;
}
