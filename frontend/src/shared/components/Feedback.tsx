import type { ReactNode } from "react";
import { Icon } from "./Icon";

export function Spinner({ className = "size-5" }: Readonly<{ className?: string }>) {
  return (
    <span
      aria-hidden="true"
      className={`inline-block animate-spin rounded-full border-2 border-current border-r-transparent ${className}`}
    />
  );
}

export function LoadingState({ label = "Loading" }: Readonly<{ label?: string }>) {
  return (
    <div role="status" className="flex min-h-48 items-center justify-center gap-3 text-slate-500">
      <Spinner />
      <span>{label}…</span>
    </div>
  );
}

export function ErrorState({
  error,
  title = "Something went wrong",
  retry,
}: Readonly<{ error: Error; title?: string; retry?: () => void }>) {
  return (
    <div
      role="alert"
      className="flex flex-col gap-3 rounded-2xl border border-rose-200 bg-rose-50 p-5 text-rose-900 sm:flex-row sm:items-center sm:justify-between"
    >
      <div className="flex items-start gap-3">
        <Icon name="alert" className="mt-0.5 size-5 shrink-0" />
        <div>
          <p className="font-semibold">{title}</p>
          <p className="text-sm text-rose-800">{error.message}</p>
        </div>
      </div>
      {retry && (
        <button type="button" className="btn btn-secondary btn-sm shrink-0" onClick={retry}>
          <Icon name="refresh" className="size-4" /> Try again
        </button>
      )}
    </div>
  );
}

export function EmptyState({
  title,
  detail,
  action,
  icon = "package",
}: Readonly<{ title: string; detail?: string; action?: ReactNode; icon?: "package" | "search" | "cart" }>) {
  return (
    <div className="card flex flex-col items-center px-6 py-16 text-center">
      <span className="mb-4 flex size-14 items-center justify-center rounded-full bg-emerald-50 text-emerald-600">
        <Icon name={icon} className="size-7" />
      </span>
      <h2 className="text-xl font-semibold">{title}</h2>
      {detail && <p className="mt-1.5 max-w-md text-slate-500">{detail}</p>}
      {action && <div className="mt-6">{action}</div>}
    </div>
  );
}

export function ProductGridSkeleton({ count = 8 }: Readonly<{ count?: number }>) {
  return (
    <div role="status" aria-label="Loading products" className="grid grid-cols-2 gap-4 md:grid-cols-3 lg:grid-cols-4">
      {Array.from({ length: count }, (_, index) => (
        <div key={index} className="card animate-pulse overflow-hidden">
          <div className="aspect-square bg-slate-100" />
          <div className="space-y-2.5 p-4">
            <div className="h-3 w-1/3 rounded bg-slate-100" />
            <div className="h-4 w-3/4 rounded bg-slate-200" />
            <div className="h-3 w-full rounded bg-slate-100" />
            <div className="h-5 w-1/4 rounded bg-slate-200" />
          </div>
        </div>
      ))}
    </div>
  );
}
