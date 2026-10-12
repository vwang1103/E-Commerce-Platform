"use client";

import { useNow } from "@/shared/hooks/useNow";
import { formatCountdown, parseServerDate } from "@/shared/lib/format";
import { Icon } from "@/shared/components/Icon";

/** Ticking countdown to an order's payment deadline. */
export function PaymentCountdown({ deadline, compact = false }: Readonly<{ deadline: string; compact?: boolean }>) {
  const now = useNow();
  const remaining = parseServerDate(deadline).getTime() - now;
  const closed = remaining <= 0;
  const urgent = !closed && remaining < 120_000;
  const tone = closed ? "text-slate-500" : urgent ? "text-rose-600" : "text-amber-700";

  return (
    <span className={`inline-flex items-center gap-1.5 text-sm font-medium ${tone}`}>
      <Icon name="clock" className="size-4" />
      {closed ? (
        "Payment window closed"
      ) : (
        <>
          {compact ? "" : "Pay within "}
          <span className="tabular-nums">{formatCountdown(remaining)}</span>
        </>
      )}
    </span>
  );
}
