"use client";

import { usePathname, useRouter, useSearchParams } from "next/navigation";
import { Suspense, useMemo, useState } from "react";
import { Breadcrumbs } from "@/shared/components/Breadcrumbs";
import { EmptyState, ErrorState, ProductGridSkeleton } from "@/shared/components/Feedback";
import { Icon } from "@/shared/components/Icon";
import { Pagination } from "@/shared/components/Pagination";
import { ProductGrid } from "@/features/catalog/components/ProductCard";
import { useAsync } from "@/shared/hooks/useAsync";
import { useCategories } from "@/features/catalog/hooks/useCategories";
import { useStock } from "@/features/inventory/hooks/useStock";
import {
  PRICE_BUCKETS,
  SORT_OPTIONS,
  bucketById,
  countByCategory,
  filterProducts,
  isSortKey,
  sortProducts,
} from "@/features/catalog/lib/browse";
import { loadCatalog } from "@/features/catalog/lib/catalog";
import { pluralize } from "@/shared/lib/format";

const PAGE_SIZE = 12;

function Shop() {
  const params = useSearchParams();
  const router = useRouter();
  const pathname = usePathname();
  const [filtersOpen, setFiltersOpen] = useState(false);
  const { categories, bySlug, loaded: categoriesLoaded } = useCategories();

  const category = bySlug(params.get("category"));
  // A link to a category we have not heard of yet must wait, or it would flash the whole catalog.
  const resolvingCategory = Boolean(params.get("category")) && !category && !categoriesLoaded;
  const bucket = bucketById(params.get("price"));
  const inStockOnly = params.get("stock") === "in";
  const sortParam = params.get("sort");
  const sort = isSortKey(sortParam) ? sortParam : "featured";
  const requestedPage = Math.max(1, Math.floor(Number(params.get("page"))) || 1) - 1;

  const catalog = useAsync(loadCatalog, []);
  const stock = useStock((catalog.data ?? []).map((product) => product.productId));
  const counts = useMemo(() => countByCategory(catalog.data ?? []), [catalog.data]);
  const visible = useMemo(() => {
    const matching = sortProducts(filterProducts(catalog.data ?? [], { categoryId: category?.id, bucket }), sort);
    return inStockOnly && stock ? matching.filter((product) => (stock.get(product.productId) ?? 0) > 0) : matching;
  }, [catalog.data, category?.id, bucket, sort, inStockOnly, stock]);
  const totalPages = Math.max(1, Math.ceil(visible.length / PAGE_SIZE));
  const page = Math.min(requestedPage, totalPages - 1);
  const pageItems = visible.slice(page * PAGE_SIZE, (page + 1) * PAGE_SIZE);
  const filtered = Boolean(category || bucket || inStockOnly);

  function update(changes: Record<string, string | null>) {
    const next = new URLSearchParams(params.toString());
    if (!("page" in changes)) next.delete("page");
    for (const [key, value] of Object.entries(changes)) {
      if (value === null || value === "" || (key === "sort" && value === "featured")) next.delete(key);
      else next.set(key, value);
    }
    const query = next.toString();
    router.replace(query ? `${pathname}?${query}` : pathname, { scroll: false });
  }

  return (
    <div className="container-page py-8">
      <Breadcrumbs
        items={[
          { label: "Home", href: "/" },
          { label: "Shop", href: category ? "/shop/" : undefined },
          ...(category ? [{ label: category.name }] : []),
        ]}
      />

      <div className="flex flex-wrap items-end justify-between gap-4">
        <div>
          <h1 className="page-title">{category?.name ?? "All products"}</h1>
          <p className="mt-1 text-slate-500" aria-live="polite">
            {catalog.data && !resolvingCategory ? pluralize(visible.length, "product") : "Loading the catalog"}
          </p>
        </div>
        <div className="flex items-center gap-3">
          <button
            type="button"
            className="btn btn-secondary btn-sm lg:hidden"
            aria-expanded={filtersOpen}
            onClick={() => setFiltersOpen((open) => !open)}
          >
            <Icon name="filter" className="size-4" /> Filters{filtered && " (on)"}
          </button>
          <label className="flex items-center gap-2 text-sm">
            <span className="sr-only text-slate-500 sm:not-sr-only">Sort by</span>
            <select className="input h-9 w-auto pr-8 text-sm" value={sort} onChange={(event) => update({ sort: event.target.value })}>
              {SORT_OPTIONS.map((option) => (
                <option key={option.value} value={option.value}>
                  {option.label}
                </option>
              ))}
            </select>
          </label>
        </div>
      </div>

      <div className="mt-8 grid gap-8 lg:grid-cols-[15rem_1fr]">
        <aside className={`${filtersOpen ? "block" : "hidden"} space-y-7 lg:block`} aria-label="Filters">
          <fieldset>
            <legend className="mb-3 text-sm font-semibold">Category</legend>
            <div className="grid gap-1">
              <FilterOption name="category" label="All categories" checked={!category} onSelect={() => update({ category: null })} />
              {categories.map((entry) => (
                <FilterOption
                  key={entry.id}
                  name="category"
                  label={entry.name}
                  count={catalog.data ? (counts.get(entry.id) ?? 0) : undefined}
                  checked={category?.id === entry.id}
                  onSelect={() => update({ category: entry.slug })}
                />
              ))}
            </div>
          </fieldset>
          <fieldset>
            <legend className="mb-3 text-sm font-semibold">Price</legend>
            <div className="grid gap-1">
              <FilterOption name="price" label="Any price" checked={!bucket} onSelect={() => update({ price: null })} />
              {PRICE_BUCKETS.map((entry) => (
                <FilterOption
                  key={entry.id}
                  name="price"
                  label={entry.label}
                  checked={bucket?.id === entry.id}
                  onSelect={() => update({ price: entry.id })}
                />
              ))}
            </div>
          </fieldset>
          <fieldset>
            <legend className="mb-3 text-sm font-semibold">Availability</legend>
            <label className="flex cursor-pointer items-center gap-2.5 rounded-lg px-2 py-1.5 text-sm hover:bg-slate-100">
              <input
                type="checkbox"
                className="size-4 accent-emerald-600"
                checked={inStockOnly}
                onChange={(event) => update({ stock: event.target.checked ? "in" : null })}
              />
              <span>In stock only</span>
            </label>
          </fieldset>
          {filtered && (
            <button type="button" className="btn btn-secondary btn-sm w-full" onClick={() => update({ category: null, price: null, stock: null })}>
              Clear filters
            </button>
          )}
        </aside>

        <section aria-label="Products">
          {catalog.error ? (
            <ErrorState error={catalog.error} title="We couldn't load the catalog" retry={catalog.reload} />
          ) : !catalog.data || resolvingCategory ? (
            <ProductGridSkeleton count={6} />
          ) : visible.length === 0 ? (
            <EmptyState
              icon="search"
              title="No products match these filters"
              detail="Try a different category or price range."
              action={
                <button type="button" className="btn btn-primary" onClick={() => update({ category: null, price: null, stock: null })}>
                  Clear filters
                </button>
              }
            />
          ) : (
            <>
              <ProductGrid products={pageItems} className="grid-cols-2 xl:grid-cols-3" />
              <Pagination page={page} totalPages={totalPages} onChange={(next) => update({ page: next === 0 ? null : String(next + 1) })} />
            </>
          )}
        </section>
      </div>
    </div>
  );
}

function FilterOption({
  name,
  label,
  count,
  checked,
  onSelect,
}: Readonly<{ name: string; label: string; count?: number; checked: boolean; onSelect: () => void }>) {
  return (
    <label className="flex cursor-pointer items-center gap-2.5 rounded-lg px-2 py-1.5 text-sm hover:bg-slate-100">
      <input type="radio" name={name} className="size-4 accent-emerald-600" checked={checked} onChange={onSelect} />
      <span className="flex-1">{label}</span>
      {count !== undefined && <span className="text-xs text-slate-400">{count}</span>}
    </label>
  );
}

export default function ShopPage() {
  return (
    <Suspense
      fallback={
        <div className="container-page py-8">
          <ProductGridSkeleton count={6} />
        </div>
      }
    >
      <Shop />
    </Suspense>
  );
}
