export const LOW_STOCK_THRESHOLD = 5;

export function StockBadge({ available }: Readonly<{ available: number | null | undefined }>) {
  if (available === undefined) return null;
  const { text, tone } =
    available === null || available <= 0
      ? { text: "Out of stock", tone: "bg-rose-50 text-rose-700" }
      : available <= LOW_STOCK_THRESHOLD
        ? { text: `Only ${available} left`, tone: "bg-amber-50 text-amber-800" }
        : { text: "In stock", tone: "bg-emerald-50 text-emerald-700" };
  return (
    <span className={`inline-flex items-center gap-1.5 rounded-full px-3 py-1 text-xs font-semibold ${tone}`}>
      <span aria-hidden="true" className="size-1.5 rounded-full bg-current" />
      {text}
    </span>
  );
}
