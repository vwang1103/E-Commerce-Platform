import type { OrderStatus } from "@/shared/lib/types";
import { Icon } from "@/shared/components/Icon";

type StepState = "done" | "current" | "upcoming" | "failed";

export function timelineSteps(status: OrderStatus): Array<{ label: string; state: StepState }> {
  const closedLabel = status === "EXPIRED" ? "Expired" : status === "REJECTED" ? "Unavailable" : "Failed";

  switch (status) {
    case "ACCEPTED":
      return [
        { label: "Order placed", state: "done" },
        { label: "Confirming stock", state: "current" },
        { label: "Payment", state: "upcoming" },
      ];
    case "PENDING_PAYMENT":
      return [
        { label: "Order placed", state: "done" },
        { label: "Stock reserved", state: "done" },
        { label: "Payment", state: "current" },
      ];
    case "PAID":
      return [
        { label: "Order placed", state: "done" },
        { label: "Stock reserved", state: "done" },
        { label: "Paid", state: "done" },
      ];
    case "CONFIRMING_INVENTORY":
      return [
        { label: "Order placed", state: "done" },
        { label: "Payment received", state: "done" },
        { label: "Confirming stock", state: "current" },
      ];
    case "INVENTORY_CONFIRMATION_FAILED":
      return [
        { label: "Order placed", state: "done" },
        { label: "Payment received", state: "done" },
        { label: "Stock confirmation needs recovery", state: "failed" },
      ];
    case "EXPIRED":
      return [
        { label: "Order placed", state: "done" },
        { label: "Stock reserved", state: "done" },
        { label: closedLabel, state: "failed" },
      ];
    default:
      return [
        { label: "Order placed", state: "done" },
        { label: closedLabel, state: "failed" },
      ];
  }
}

const DOT: Record<StepState, string> = {
  done: "bg-emerald-600 text-white",
  current: "bg-emerald-600 text-white ring-4 ring-emerald-100",
  upcoming: "bg-slate-200 text-slate-500",
  failed: "bg-rose-600 text-white",
};

export function OrderTimeline({ status }: Readonly<{ status: OrderStatus }>) {
  const steps = timelineSteps(status);
  return (
    <ol aria-label="Order progress" className="flex items-start">
      {steps.map((step, index) => (
        <li key={step.label} className="relative flex flex-1 flex-col items-center text-center">
          {index > 0 && (
            <span
              aria-hidden="true"
              className={`absolute top-4 right-1/2 h-0.5 w-full -translate-y-1/2 ${step.state === "upcoming" ? "bg-slate-200" : step.state === "failed" ? "bg-rose-300" : "bg-emerald-600"}`}
            />
          )}
          <span className={`relative z-10 flex size-8 items-center justify-center rounded-full text-sm font-semibold ${DOT[step.state]}`}>
            {step.state === "done" ? (
              <Icon name="check" className="size-4" />
            ) : step.state === "failed" ? (
              <Icon name="close" className="size-4" />
            ) : (
              index + 1
            )}
          </span>
          <span className={`mt-2 text-xs font-medium ${step.state === "upcoming" ? "text-slate-400" : "text-slate-700"}`}>
            {step.label}
            <span className="sr-only">{` (${step.state})`}</span>
          </span>
        </li>
      ))}
    </ol>
  );
}
