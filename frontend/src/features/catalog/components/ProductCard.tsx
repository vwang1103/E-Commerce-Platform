"use client";

import Link from "next/link";
import { useCart } from "@/features/cart/hooks/useCart";
import { useCategories } from "@/features/catalog/hooks/useCategories";
import { useStock } from "@/features/inventory/hooks/useStock";
import { toCartLine } from "@/features/cart/lib/cart";
import { formatMoney } from "@/shared/lib/format";
import type { ProductSummary } from "@/shared/lib/types";
import { LOW_STOCK_THRESHOLD } from "@/features/inventory/components/StockBadge";
import { Icon } from "@/shared/components/Icon";
import { ProductArtwork } from "./ProductArtwork";

/** `available` is the live stock level; leave it undefined while unknown. */
export function ProductCard({ product, available }: Readonly<{ product: ProductSummary; available?: number }>) {
  const { add, setDrawerOpen } = useCart();
  const { byId } = useCategories();
  const href = `/product/?id=${product.productId}`;
  const soldOut = available === 0;
  const purchasable = product.status === "ACTIVE" && !soldOut;
  const lowStock = available !== undefined && available > 0 && available <= LOW_STOCK_THRESHOLD;

  return (
    <article className="group relative flex min-w-0 flex-col">
      <Link href={href} aria-label={`View ${product.name}`} tabIndex={-1} className="relative block">
        <ProductArtwork productId={product.productId} name={product.name} categoryId={product.categoryId} className={`aspect-square rounded-xl ${soldOut ? "opacity-60 grayscale" : ""}`} />
        {(soldOut || lowStock) && (
          <span
            className={`absolute top-3 left-3 rounded-full px-2.5 py-1 text-xs font-semibold shadow-sm ${soldOut ? "bg-slate-900 text-white" : "bg-amber-100 text-amber-900"}`}
          >
            {soldOut ? "Sold out" : `Only ${available} left`}
          </span>
        )}
      </Link>
      <div className="flex flex-1 flex-col pt-3">
        <p className="text-[11px] font-semibold tracking-wide text-emerald-600 uppercase">{byId(product.categoryId).name}</p>
        <h3 className="mt-1 leading-snug font-medium text-slate-800">
          <Link href={href} className="after:absolute after:inset-0 after:content-[''] hover:text-emerald-700">
            {product.name}
          </Link>
        </h3>
        <p className="mt-1 line-clamp-2 text-sm leading-5 text-slate-500">{product.description}</p>
        <div className="mt-auto flex items-center justify-between pt-4">
          <span className="text-base font-semibold tabular-nums">{formatMoney(product.priceMinor, product.currency)}</span>
          <button
            type="button"
            className="btn relative z-10 size-9 bg-emerald-600 p-0 text-white hover:bg-emerald-700"
            disabled={!purchasable}
            aria-label={soldOut ? `${product.name} is sold out` : `Add ${product.name} to cart`}
            onClick={() => {
              add(toCartLine(product));
              setDrawerOpen(true);
            }}
          >
            <Icon name="plus" className="size-5" />
          </button>
        </div>
      </div>
    </article>
  );
}

/** Renders cards and fetches live stock for all of them in a single request. */
export function ProductGrid({
  products,
  className = "grid-cols-2 md:grid-cols-3 lg:grid-cols-4",
}: Readonly<{ products: readonly ProductSummary[]; className?: string }>) {
  const stock = useStock(products.map((product) => product.productId));
  return (
    <div className={`grid gap-4 ${className}`}>
      {products.map((product) => (
        <ProductCard key={product.productId} product={product} available={stock?.get(product.productId)} />
      ))}
    </div>
  );
}
