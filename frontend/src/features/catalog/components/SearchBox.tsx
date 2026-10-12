"use client";

import { useRouter } from "next/navigation";
import { useEffect, useId, useRef, useState, type FormEvent, type KeyboardEvent } from "react";
import { useAsync } from "@/shared/hooks/useAsync";
import { api } from "@/shared/lib/api";
import { formatMoney } from "@/shared/lib/format";
import { Icon } from "@/shared/components/Icon";
import { ProductArtwork } from "./ProductArtwork";

const MIN_QUERY_LENGTH = 2;

export function SearchBox({ className = "" }: Readonly<{ className?: string }>) {
  const router = useRouter();
  const listId = useId();
  const containerRef = useRef<HTMLDivElement>(null);
  const [value, setValue] = useState("");
  const [debounced, setDebounced] = useState("");
  const [open, setOpen] = useState(false);
  const [active, setActive] = useState(-1);

  useEffect(() => {
    const timer = window.setTimeout(() => setDebounced(value.trim()), 250);
    return () => window.clearTimeout(timer);
  }, [value]);

  const query = value.trim();
  const suggestions = useAsync(
    () => (debounced.length >= MIN_QUERY_LENGTH ? api.searchProducts(debounced, 0, 5) : Promise.resolve(undefined)),
    [debounced],
  );
  const settled = debounced === query && query.length >= MIN_QUERY_LENGTH;
  const items = settled ? (suggestions.data?.items ?? []) : [];
  const showPanel = open && query.length >= MIN_QUERY_LENGTH;
  const optionCount = items.length + 1;

  function goToResults() {
    if (!query) return;
    setOpen(false);
    router.push(`/search/?q=${encodeURIComponent(query)}`);
  }

  function goToProduct(productId: number) {
    setOpen(false);
    setValue("");
    router.push(`/product/?id=${productId}`);
  }

  function submit(event: FormEvent) {
    event.preventDefault();
    if (active >= 0 && active < items.length) goToProduct(items[active].productId);
    else goToResults();
  }

  function onKeyDown(event: KeyboardEvent<HTMLInputElement>) {
    if (event.key === "Escape") {
      setOpen(false);
    } else if (event.key === "ArrowDown" || event.key === "ArrowUp") {
      event.preventDefault();
      setOpen(true);
      const step = event.key === "ArrowDown" ? 1 : -1;
      // Positions run from -1 (the input itself) through the last option.
      const positions = optionCount + 1;
      setActive((current) => ((current + 1 + step + positions) % positions) - 1);
    }
  }

  return (
    <div
      ref={containerRef}
      className={`relative ${className}`}
      onBlur={(event) => {
        if (!containerRef.current?.contains(event.relatedTarget)) setOpen(false);
      }}
    >
      <form role="search" onSubmit={submit} className="relative">
        <label htmlFor={`${listId}-input`} className="sr-only">
          Search products
        </label>
        <Icon name="search" className="pointer-events-none absolute top-1/2 left-4 size-5 -translate-y-1/2 text-slate-400" />
        <input
          id={`${listId}-input`}
          type="search"
          role="combobox"
          aria-expanded={showPanel}
          aria-controls={listId}
          aria-autocomplete="list"
          aria-activedescendant={showPanel && active >= 0 ? `${listId}-${active}` : undefined}
          autoComplete="off"
          maxLength={100}
          placeholder="Search headphones, lamps, notebooks…"
          className="h-11 w-full rounded-full border border-slate-200 bg-slate-100 pr-4 pl-11 text-sm placeholder:text-slate-500 focus:border-emerald-500 focus:bg-white focus:outline-2 focus:outline-offset-0 focus:outline-emerald-100"
          value={value}
          onChange={(event) => {
            setValue(event.target.value);
            setOpen(true);
            setActive(-1);
          }}
          onFocus={() => setOpen(true)}
          onKeyDown={onKeyDown}
        />
      </form>

      {showPanel && (
        <ul
          id={listId}
          role="listbox"
          aria-label="Search suggestions"
          onMouseDown={(event) => event.preventDefault()}
          className="absolute inset-x-0 top-full z-50 mt-2 animate-fade-in overflow-hidden rounded-2xl border border-slate-200 bg-white py-1.5 shadow-xl"
        >
          {!settled || suggestions.loading ? (
            <li role="presentation" className="px-4 py-3 text-sm text-slate-500">
              Searching…
            </li>
          ) : suggestions.error ? (
            <li role="presentation" className="px-4 py-3 text-sm text-rose-600">
              Suggestions are unavailable right now.
            </li>
          ) : (
            items.map((item, index) => (
              <li
                key={item.productId}
                id={`${listId}-${index}`}
                role="option"
                aria-selected={active === index}
                onClick={() => goToProduct(item.productId)}
                className={`flex cursor-pointer items-center gap-3 px-3 py-2 ${active === index ? "bg-emerald-50" : "hover:bg-slate-50"}`}
              >
                <ProductArtwork productId={item.productId} name={item.name} categoryId={item.categoryId} className="size-11 shrink-0 rounded-lg" />
                <span className="min-w-0 flex-1 truncate text-sm font-medium">{item.name}</span>
                <span className="text-sm text-slate-500 tabular-nums">{formatMoney(item.priceMinor, item.currency)}</span>
              </li>
            ))
          )}
          {settled && !suggestions.loading && (
            <li
              id={`${listId}-${items.length}`}
              role="option"
              aria-selected={active === items.length}
              onClick={goToResults}
              className={`flex cursor-pointer items-center gap-2 border-t border-slate-100 px-4 py-2.5 text-sm font-medium text-emerald-700 ${active === items.length ? "bg-emerald-50" : "hover:bg-slate-50"}`}
            >
              <Icon name="search" className="size-4" />
              {items.length === 0 && !suggestions.error ? `No quick matches. Search for “${query}”` : `See all results for “${query}”`}
            </li>
          )}
        </ul>
      )}
    </div>
  );
}
