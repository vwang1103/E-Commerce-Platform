"use client";

import Link from "next/link";
import { useRouter, useSearchParams } from "next/navigation";
import { Suspense, useMemo, useState, type FormEvent } from "react";
import { EmptyState, ErrorState, ProductGridSkeleton } from "@/shared/components/Feedback";
import { Icon } from "@/shared/components/Icon";
import { Pagination } from "@/shared/components/Pagination";
import { ProductGrid } from "@/features/catalog/components/ProductCard";
import { useAsync } from "@/shared/hooks/useAsync";
import { useCategories } from "@/features/catalog/hooks/useCategories";
import { api } from "@/shared/lib/api";
import { SORT_OPTIONS, filterProducts, isSortKey, sortProducts, type SortKey } from "@/features/catalog/lib/browse";
import { pluralize } from "@/shared/lib/format";

const PAGE_SIZE = 12;
const SUGGESTED_SEARCHES = ["headphones", "keyboard", "desk lamp", "notebook", "coffee", "charger"];

function SearchView({ query, page }: Readonly<{ query: string; page: number }>) {
  const router = useRouter();
  const { categories } = useCategories();
  const [draft, setDraft] = useState(query);
  const [sort, setSort] = useState<SortKey>("featured");
  const [categoryId, setCategoryId] = useState<number>();

  const results = useAsync(
    () => (query ? api.searchProducts(query, page, PAGE_SIZE) : Promise.resolve(undefined)),
    [query, page],
  );

  const visible = useMemo(
    () => sortProducts(filterProducts(results.data?.items ?? [], { categoryId }), sort),
    [results.data, categoryId, sort],
  );
  const presentCategories = useMemo(
    () => categories.filter((category) => results.data?.items.some((item) => item.categoryId === category.id)),
    [categories, results.data],
  );

  function go(nextQuery: string, nextPage = 0) {
    const params = new URLSearchParams({ q: nextQuery });
    if (nextPage > 0) params.set("page", String(nextPage + 1));
    router.push(`/search/?${params}`);
  }

  function submit(event: FormEvent) {
    event.preventDefault();
    const normalized = draft.trim();
    if (normalized) go(normalized);
  }

  return (
    <div className="container-page py-10">
      <h1 className="page-title">{query ? <>Results for “{query}”</> : "Search the catalog"}</h1>

      <form role="search" onSubmit={submit} className="mt-6 flex max-w-2xl gap-2">
        <label htmlFor="page-search" className="sr-only">
          Search products
        </label>
        <div className="relative flex-1">
          <Icon name="search" className="pointer-events-none absolute top-1/2 left-4 size-5 -translate-y-1/2 text-slate-400" />
          <input
            id="page-search"
            className="input h-12 rounded-full pl-11"
            maxLength={100}
            placeholder="Search by name or description"
            value={draft}
            onChange={(event) => setDraft(event.target.value)}
          />
        </div>
        <button type="submit" className="btn btn-primary h-12" disabled={!draft.trim()}>
          Search
        </button>
      </form>

      {!query ? (
        <div className="mt-10">
          <p className="text-sm font-medium text-slate-500">Popular searches</p>
          <div className="mt-3 flex flex-wrap gap-2">
            {SUGGESTED_SEARCHES.map((term) => (
              <Link key={term} href={`/search/?q=${encodeURIComponent(term)}`} className="chip">
                {term}
              </Link>
            ))}
          </div>
        </div>
      ) : results.error ? (
        <div className="mt-8">
          <ErrorState error={results.error} title="Search is unavailable" retry={results.reload} />
        </div>
      ) : !results.data ? (
        <div className="mt-8">
          <ProductGridSkeleton count={4} />
        </div>
      ) : (
        <>
          <div className="mt-8 flex flex-wrap items-center justify-between gap-3">
            <p className="text-slate-600" aria-live="polite">
              <strong className="text-slate-900">{pluralize(results.data.totalElements, "result")}</strong>
              <span className="text-slate-400"> · {results.data.tookMillis} ms</span>
            </p>
            {results.data.items.length > 0 && (
              <label className="flex items-center gap-2 text-sm">
                <span className="text-slate-500">Sort this page</span>
                <select
                  className="input h-9 w-auto pr-8 text-sm"
                  value={sort}
                  onChange={(event) => isSortKey(event.target.value) && setSort(event.target.value)}
                >
                  <option value="featured">Best match</option>
                  {SORT_OPTIONS.filter((option) => option.value !== "featured").map((option) => (
                    <option key={option.value} value={option.value}>
                      {option.label}
                    </option>
                  ))}
                </select>
              </label>
            )}
          </div>

          {presentCategories.length > 1 && (
            <div className="mt-4 flex flex-wrap gap-2" role="group" aria-label="Filter results by category">
              <button type="button" className="chip" aria-pressed={categoryId === undefined} onClick={() => setCategoryId(undefined)}>
                All
              </button>
              {presentCategories.map((category) => (
                <button
                  key={category.id}
                  type="button"
                  className="chip"
                  aria-pressed={categoryId === category.id}
                  onClick={() => setCategoryId(category.id)}
                >
                  {category.name}
                </button>
              ))}
            </div>
          )}

          <div className="mt-6">
            {results.data.items.length === 0 ? (
              <EmptyState
                icon="search"
                title={`No matches for “${query}”`}
                detail="Check the spelling, try a broader term, or browse the full catalog. Newly listed products can take a few seconds to appear."
                action={
                  <Link href="/shop/" className="btn btn-primary">
                    Browse all products
                  </Link>
                }
              />
            ) : (
              <>
                <ProductGrid products={visible} />
                <Pagination page={page} totalPages={results.data.totalPages} onChange={(next) => go(query, next)} />
              </>
            )}
          </div>
        </>
      )}
    </div>
  );
}

function SearchRoute() {
  const params = useSearchParams();
  const query = (params.get("q") ?? "").trim().slice(0, 100);
  const page = Math.max(1, Math.floor(Number(params.get("page"))) || 1) - 1;
  // Keyed by query so the draft, sort, and category filter reset for each new search.
  return <SearchView key={query} query={query} page={page} />;
}

export default function SearchPage() {
  return (
    <Suspense
      fallback={
        <div className="container-page py-10">
          <ProductGridSkeleton count={4} />
        </div>
      }
    >
      <SearchRoute />
    </Suspense>
  );
}
