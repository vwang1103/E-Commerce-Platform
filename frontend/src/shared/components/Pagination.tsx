import { pageWindow } from "@/features/catalog/lib/browse";
import { Icon } from "./Icon";

/** Zero-based pagination with numbered pages. */
export function Pagination({
  page,
  totalPages,
  onChange,
}: Readonly<{ page: number; totalPages: number; onChange: (page: number) => void }>) {
  if (totalPages <= 1) return null;
  return (
    <nav aria-label="Pagination" className="mt-10 flex items-center justify-center gap-1.5">
      <button
        type="button"
        className="btn btn-secondary btn-sm px-3"
        disabled={page === 0}
        onClick={() => onChange(page - 1)}
        aria-label="Previous page"
      >
        <Icon name="chevron-left" className="size-4" />
      </button>
      {pageWindow(page, totalPages).map((entry, index) =>
        entry === null ? (
          <span key={`gap-${index}`} className="px-1 text-slate-400" aria-hidden="true">
            …
          </span>
        ) : (
          <button
            key={entry}
            type="button"
            className="chip h-9 min-w-9 justify-center px-2"
            aria-current={entry === page ? "true" : undefined}
            aria-label={`Page ${entry + 1}`}
            onClick={() => onChange(entry)}
          >
            {entry + 1}
          </button>
        ),
      )}
      <button
        type="button"
        className="btn btn-secondary btn-sm px-3"
        disabled={page + 1 >= totalPages}
        onClick={() => onChange(page + 1)}
        aria-label="Next page"
      >
        <Icon name="chevron-right" className="size-4" />
      </button>
    </nav>
  );
}
