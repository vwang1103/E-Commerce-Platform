"use client";

import Link from "next/link";
import { useRouter, useSearchParams } from "next/navigation";
import { Suspense, useMemo, useState } from "react";
import { Breadcrumbs } from "@/shared/components/Breadcrumbs";
import { EmptyState, ErrorState, LoadingState } from "@/shared/components/Feedback";
import { Icon, type IconName } from "@/shared/components/Icon";
import { ProductArtwork } from "@/features/catalog/components/ProductArtwork";
import { ProductGrid } from "@/features/catalog/components/ProductCard";
import { QuantityStepper } from "@/shared/components/QuantityStepper";
import { StockBadge } from "@/features/inventory/components/StockBadge";
import { useAsync } from "@/shared/hooks/useAsync";
import { useCart } from "@/features/cart/hooks/useCart";
import { useCategories } from "@/features/catalog/hooks/useCategories";
import { api, isNotFound } from "@/shared/lib/api";
import { MAX_LINE_QUANTITY, toCartLine } from "@/features/cart/lib/cart";
import { loadCatalog } from "@/features/catalog/lib/catalog";
import { formatDate, formatMoney } from "@/shared/lib/format";

const PERKS: ReadonlyArray<{ icon: IconName; text: string }> = [
  { icon: "shield", text: "Stock is reserved for you the moment you check out" },
  { icon: "clock", text: "15 minutes to complete payment, with a live countdown" },
  { icon: "package", text: "Track every order from placed to paid" },
];

/** Loads one product and its inventory, then coordinates cart and buy-now actions. */
function ProductDetail({ productId }: Readonly<{ productId: number }>) {
  const router = useRouter();
  const { lines, add, setDrawerOpen } = useCart();
  const { byId } = useCategories();
  const [quantity, setQuantity] = useState(1);

  const product = useAsync(
    () => (productId > 0 ? api.getProduct(productId) : Promise.reject(new Error("Choose a product from the catalog."))),
    [productId],
  );
  const inventory = useAsync(async () => {
    if (productId <= 0) return undefined;
    try {
      return await api.getInventory(productId);
    } catch (reason) {
      if (isNotFound(reason)) return null;
      throw reason;
    }
  }, [productId]);
  const catalog = useAsync(loadCatalog, []);

  const related = useMemo(
    () => (catalog.data ?? []).filter((item) => item.categoryId === product.data?.categoryId && item.productId !== productId).slice(0, 4),
    [catalog.data, product.data?.categoryId, productId],
  );

  if (product.error) {
    return isNotFound(product.error) ? (
      <EmptyState
        icon="search"
        title="We couldn't find that product"
        detail="It may have been removed or is no longer for sale."
        action={
          <Link href="/shop/" className="btn btn-primary">
            Browse all products
          </Link>
        }
      />
    ) : (
      <ErrorState error={product.error} title="We couldn't load this product" retry={product.reload} />
    );
  }
  if (!product.data) return <LoadingState label="Loading product" />;

  const item = product.data;
  const category = byId(item.categoryId);
  // 404 means no stock record exists, so the item cannot be reserved. Other failures leave stock unknown.
  const available = inventory.data === undefined ? undefined : inventory.data === null ? 0 : inventory.data.availableQuantity;
  const soldOut = item.status !== "ACTIVE" || available === 0;
  const maxQuantity = Math.max(1, Math.min(MAX_LINE_QUANTITY, available ?? MAX_LINE_QUANTITY));
  const selected = Math.min(quantity, maxQuantity);
  const inCart = lines.find((line) => line.productId === item.productId)?.quantity ?? 0;

  function addToCart() {
    add(toCartLine(item, selected));
    setDrawerOpen(true);
  }

  function buyNow() {
    add(toCartLine(item, selected));
    router.push("/checkout/");
  }

  return (
    <>
      <Breadcrumbs
        items={[
          { label: "Home", href: "/" },
          { label: category.name, href: `/shop/?category=${category.slug}` },
          { label: item.name },
        ]}
      />

      <div className="grid gap-10 lg:grid-cols-2">
        <div className="group lg:sticky lg:top-40 lg:self-start">
          <ProductArtwork productId={item.productId} name={item.name} categoryId={item.categoryId} className="aspect-square rounded-3xl ring-1 ring-slate-200" />
        </div>

        <div>
          <Link href={`/shop/?category=${category.slug}`} className="eyebrow hover:underline">
            {category.name}
          </Link>
          <h1 className="mt-2 text-3xl font-bold tracking-tight sm:text-4xl">{item.name}</h1>
          <div className="mt-4 flex flex-wrap items-center gap-3">
            <p className="text-3xl font-bold tabular-nums">{formatMoney(item.priceMinor, item.currency)}</p>
            {item.status !== "ACTIVE" ? (
              <span className="rounded-full bg-slate-100 px-3 py-1 text-xs font-semibold text-slate-600">Not for sale</span>
            ) : (
              <StockBadge available={available} />
            )}
          </div>
          <p className="mt-5 leading-relaxed text-slate-600">{item.description}</p>

          <div className="card mt-8 space-y-4 p-5">
            <div className="flex flex-wrap items-center justify-between gap-4">
              <div>
                <p className="text-sm font-medium">Quantity</p>
                {inCart > 0 && <p className="text-xs text-slate-500">{inCart} already in your cart</p>}
              </div>
              <QuantityStepper value={selected} onChange={setQuantity} max={maxQuantity} label="Quantity" />
            </div>
            <div className="grid gap-3 sm:grid-cols-2">
              <button type="button" className="btn btn-secondary" disabled={soldOut} onClick={addToCart}>
                <Icon name="cart" className="size-5" /> Add to cart
              </button>
              <button type="button" className="btn btn-primary" disabled={soldOut} onClick={buyNow}>
                {soldOut ? "Unavailable" : "Buy now"}
              </button>
            </div>
            <p className="text-sm text-slate-500">
              Total <strong className="text-slate-900 tabular-nums">{formatMoney(item.priceMinor * selected, item.currency)}</strong>
            </p>
          </div>

          <ul className="mt-6 grid gap-3 text-sm text-slate-600">
            {PERKS.map((perk) => (
              <li key={perk.text} className="flex items-center gap-3">
                <span className="flex size-8 shrink-0 items-center justify-center rounded-lg bg-emerald-50 text-emerald-600">
                  <Icon name={perk.icon} className="size-4" />
                </span>
                {perk.text}
              </li>
            ))}
          </ul>

          <dl className="mt-8 divide-y divide-slate-100 rounded-2xl border border-slate-200 bg-white text-sm">
            <Detail label="Category" value={category.name} />
            <Detail label="Availability" value={available === undefined ? "Checking…" : available === 0 ? "Out of stock" : "Available now"} />
            {item.createdAt && <Detail label="Listed" value={formatDate(item.createdAt)} />}
          </dl>
        </div>
      </div>

      {related.length > 0 && (
        <section className="mt-20" aria-labelledby="related-heading">
          <h2 id="related-heading" className="mb-6 text-2xl font-bold tracking-tight">
            More in {category.name}
          </h2>
          <ProductGrid products={related} />
        </section>
      )}
    </>
  );
}

function Detail({ label, value }: Readonly<{ label: string; value: string }>) {
  return (
    <div className="flex justify-between gap-4 px-4 py-3">
      <dt className="text-slate-500">{label}</dt>
      <dd className="font-medium">{value}</dd>
    </div>
  );
}

function ProductRoute() {
  const params = useSearchParams();
  const id = Number(params.get("id"));
  const productId = Number.isSafeInteger(id) && id > 0 ? id : 0;
  // Keyed by product so the selected quantity resets when navigating between products.
  return <ProductDetail key={productId} productId={productId} />;
}

export default function ProductPage() {
  return (
    <div className="container-page py-8">
      <Suspense fallback={<LoadingState label="Loading product" />}>
        <ProductRoute />
      </Suspense>
    </div>
  );
}
