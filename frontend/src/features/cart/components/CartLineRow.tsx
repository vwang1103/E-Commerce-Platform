import Link from "next/link";
import type { ReactNode } from "react";
import { useCategories } from "@/features/catalog/hooks/useCategories";
import { MAX_LINE_QUANTITY, describeIssue, type CheckedLine } from "@/features/cart/lib/cart";
import { formatMoney } from "@/shared/lib/format";
import { ProductArtwork } from "@/features/catalog/components/ProductArtwork";
import { Icon } from "@/shared/components/Icon";
import { QuantityStepper } from "@/shared/components/QuantityStepper";

export function CartLineRow({
  checked,
  onQuantityChange,
  onRemove,
  note,
}: Readonly<{
  checked: CheckedLine;
  /** Omit to show a read-only quantity. */
  onQuantityChange?: (quantity: number) => void;
  onRemove?: () => void;
  note?: ReactNode;
}>) {
  const { line, issue, previousPriceMinor, unitPriceMinor, currency } = checked;
  const { byId } = useCategories();
  const href = `/product/?id=${line.productId}`;

  return (
    <li className="flex gap-4 py-5 sm:gap-5">
      <Link href={href} className="shrink-0" tabIndex={-1} aria-hidden="true">
        <ProductArtwork productId={line.productId} name={line.name} categoryId={line.categoryId} className="size-24 rounded-2xl sm:size-28" />
      </Link>
      <div className="flex min-w-0 flex-1 flex-col gap-2">
        <div className="flex items-start justify-between gap-3">
          <div className="min-w-0">
            <Link href={href} className="font-semibold hover:text-emerald-700">
              {line.name}
            </Link>
            <p className="text-sm text-slate-500">{byId(line.categoryId).name}</p>
            <p className="mt-1 text-sm text-slate-600 tabular-nums">
              {formatMoney(unitPriceMinor, currency)} each
              {previousPriceMinor !== undefined && (
                <span className="ml-2 text-xs font-medium text-amber-700">
                  Price updated from {formatMoney(previousPriceMinor, currency)}
                </span>
              )}
            </p>
          </div>
          <p className="font-bold tabular-nums">{formatMoney(unitPriceMinor * line.quantity, currency)}</p>
        </div>

        {issue && (
          <p role="alert" className="flex flex-wrap items-center gap-x-3 gap-y-1 text-sm font-medium text-rose-700">
            <span className="flex items-center gap-1.5">
              <Icon name="alert" className="size-4" /> {describeIssue(issue)}
            </span>
            {onQuantityChange && issue.kind === "insufficient" && issue.available > 0 && (
              <button type="button" className="underline hover:no-underline" onClick={() => onQuantityChange(issue.available)}>
                Reduce to {issue.available}
              </button>
            )}
          </p>
        )}
        {note}

        <div className="mt-auto flex items-center justify-between gap-3">
          {onQuantityChange ? (
            <QuantityStepper
              size="sm"
              label={`Quantity of ${line.name}`}
              value={line.quantity}
              max={MAX_LINE_QUANTITY}
              onChange={onQuantityChange}
            />
          ) : (
            <span className="text-sm text-slate-500">Qty {line.quantity}</span>
          )}
          {onRemove && (
            <button
              type="button"
              className="flex items-center gap-1 text-sm font-medium text-slate-500 hover:text-rose-600"
              onClick={onRemove}
              aria-label={`Remove ${line.name}`}
            >
              <Icon name="trash" className="size-4" /> Remove
            </button>
          )}
        </div>
      </div>
    </li>
  );
}
