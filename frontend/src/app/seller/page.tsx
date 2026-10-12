"use client";

import { useMemo, useState, type FormEvent } from "react";
import { Breadcrumbs } from "@/shared/components/Breadcrumbs";
import { EmptyState, ErrorState, LoadingState, Spinner } from "@/shared/components/Feedback";
import { Icon } from "@/shared/components/Icon";
import { Modal } from "@/shared/components/Modal";
import { Pagination } from "@/shared/components/Pagination";
import { ProductArtwork } from "@/features/catalog/components/ProductArtwork";
import { ProductFormDialog } from "@/features/seller/components/ProductFormDialog";
import { useAsync } from "@/shared/hooks/useAsync";
import { useCategories } from "@/features/catalog/hooks/useCategories";
import { useUser } from "@/shared/hooks/useUser";
import { api, isNotFound, toError } from "@/shared/lib/api";
import { invalidateCatalog } from "@/features/catalog/lib/catalog";
import { formatMoney, pluralize } from "@/shared/lib/format";
import { invalidateStock } from "@/features/inventory/lib/stock";
import { toast } from "@/shared/lib/toast";
import type { Inventory, Product } from "@/shared/lib/types";

const PAGE_SIZE = 10;

type Dialog = { kind: "form"; product?: Product } | { kind: "stock"; product: Product };

function StockDialog({ product, userId, onClose, onSaved }: Readonly<{ product: Product; userId: string; onClose: () => void; onSaved: () => void }>) {
  const [quantity, setQuantity] = useState("10");
  const [error, setError] = useState<string>();
  const [saving, setSaving] = useState(false);

  async function submit(event: FormEvent) {
    event.preventDefault();
    if (!/^\d{1,9}$/.test(quantity.trim())) {
      setError("Enter a whole number.");
      return;
    }
    setSaving(true);
    try {
      await api.createInventory(product.productId, Number(quantity), userId);
      toast("Stock added");
      onSaved();
    } catch (reason) {
      setError(toError(reason).message);
    } finally {
      setSaving(false);
    }
  }

  return (
    <Modal open onClose={onClose} title={`Add stock for ${product.name}`}>
      <form onSubmit={(event) => void submit(event)} noValidate className="space-y-4">
        <div>
          <label htmlFor="opening-stock" className="label">
            Opening stock
          </label>
          <input id="opening-stock" className="input" inputMode="numeric" value={quantity} onChange={(event) => { setQuantity(event.target.value); setError(undefined); }} autoFocus />
          {error && (
            <p role="alert" className="field-error">
              {error}
            </p>
          )}
        </div>
        <p className="text-xs text-slate-500">A stock record can be created once per product. It is held in both the database and the fast reservation store.</p>
        <div className="flex justify-end gap-3">
          <button type="button" className="btn btn-secondary" onClick={onClose}>
            Cancel
          </button>
          <button type="submit" className="btn btn-primary" disabled={saving}>
            {saving ? <Spinner className="size-4" /> : "Add stock"}
          </button>
        </div>
      </form>
    </Modal>
  );
}

function StockCell({ inventory, loading }: Readonly<{ inventory: Inventory | null | undefined; loading: boolean }>) {
  if (inventory === undefined) return <span className="text-slate-400">{loading ? "…" : "Unknown"}</span>;
  if (inventory === null) return <span className="font-medium text-amber-700">No stock record</span>;
  return (
    <div>
      <p className={`font-semibold tabular-nums ${inventory.availableQuantity === 0 ? "text-rose-700" : ""}`}>
        {inventory.availableQuantity} available
      </p>
      <p className="text-xs text-slate-500 tabular-nums">
        {inventory.reservedQuantity} held · {inventory.soldQuantity} sold · {inventory.totalQuantity} total
      </p>
    </div>
  );
}

/** Coordinates the signed-in user's product list, editing workflow, and opening stock. */
export default function SellerPage() {
  const { userId, username } = useUser();
  const { byId } = useCategories();
  const [page, setPage] = useState(0);
  const [dialog, setDialog] = useState<Dialog>();

  const products = useAsync(() => api.listMyProducts(userId), [userId]);
  const items = useMemo(() => products.data?.items ?? [], [products.data]);
  const totalPages = Math.max(1, Math.ceil(items.length / PAGE_SIZE));
  const currentPage = Math.min(page, totalPages - 1);
  const rows = useMemo(() => items.slice(currentPage * PAGE_SIZE, (currentPage + 1) * PAGE_SIZE), [items, currentPage]);
  const rowKey = rows.map((row) => row.productId).join("|");

  const stock = useAsync(async () => {
    const entries = await Promise.all(
      rows.map(async (row): Promise<[number, Inventory | null | undefined]> => {
        try {
          return [row.productId, await api.getInventory(row.productId)];
        } catch (reason) {
          return [row.productId, isNotFound(reason) ? null : undefined];
        }
      }),
    );
    return new Map(entries);
  }, [rowKey]);

  function refresh() {
    invalidateCatalog();
    invalidateStock();
    products.reload();
    stock.reload();
  }

  async function deactivate(product: Product) {
    if (!window.confirm(`Hide “${product.name}” from the store? Hidden products are no longer listed here either.`)) return;
    try {
      await api.updateProduct(product.productId, {
        categoryId: product.categoryId,
        name: product.name,
        description: product.description,
        priceMinor: product.priceMinor,
        currency: product.currency,
        status: "INACTIVE",
        version: product.version,
      }, userId);
      toast("Product hidden from the store");
      refresh();
    } catch (reason) {
      toast(toError(reason).message, "error");
    }
  }

  return (
    <div className="container-page py-8">
      <Breadcrumbs items={[{ label: "Home", href: "/" }, { label: "Seller console" }]} />

      <div className="flex flex-wrap items-end justify-between gap-4">
        <div>
          <h1 className="page-title">Seller console</h1>
          <p className="mt-1 max-w-xl text-slate-500">
            Create and manage products sold by {username}. Your buyer and seller activity use the same account.
          </p>
        </div>
        <div className="flex flex-wrap items-center gap-3">
          <button type="button" className="btn btn-primary" onClick={() => setDialog({ kind: "form" })}>
            <Icon name="plus" className="size-4" /> New product
          </button>
        </div>
      </div>

      <div className="mt-8">
        {products.error && !products.data ? (
          <ErrorState error={products.error} title="We couldn't load your products" retry={products.reload} />
        ) : !products.data ? (
          <LoadingState label="Loading products" />
        ) : items.length === 0 ? (
          <EmptyState
            icon="package"
            title="You have no active products"
            detail="Create a product with opening stock and it will appear in search within a few seconds."
            action={
              <button type="button" className="btn btn-primary" onClick={() => setDialog({ kind: "form" })}>
                Create the first product
              </button>
            }
          />
        ) : (
          <>
            <p className="mb-3 text-sm text-slate-500">{pluralize(items.length, "active product")}</p>
            <div className="card overflow-x-auto">
              <table className="w-full min-w-[40rem] text-left text-sm">
                <thead className="border-b border-slate-200 text-xs tracking-wide text-slate-500 uppercase">
                  <tr>
                    <th scope="col" className="px-5 py-3 font-semibold">Product</th>
                    <th scope="col" className="px-3 py-3 font-semibold">Price</th>
                    <th scope="col" className="px-3 py-3 font-semibold">Stock</th>
                    <th scope="col" className="px-5 py-3 text-right font-semibold">Actions</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-slate-100">
                  {rows.map((product) => (
                    <tr key={product.productId}>
                      <td className="px-5 py-3">
                        <div className="flex items-center gap-3">
                          <ProductArtwork productId={product.productId} name={product.name} categoryId={product.categoryId} className="size-12 shrink-0 rounded-lg" />
                          <div className="min-w-0">
                            <p className="truncate font-medium">{product.name}</p>
                            <p className="text-xs text-slate-500">
                              {byId(product.categoryId).name} · Product #{product.productId}
                            </p>
                          </div>
                        </div>
                      </td>
                      <td className="px-3 py-3 font-medium tabular-nums">{formatMoney(product.priceMinor, product.currency)}</td>
                      <td className="px-3 py-3">
                        <StockCell inventory={stock.data?.get(product.productId)} loading={stock.loading} />
                      </td>
                      <td className="px-5 py-3">
                        <div className="flex justify-end gap-2">
                          {stock.data?.get(product.productId) === null && (
                            <button type="button" className="btn btn-secondary btn-sm" onClick={() => setDialog({ kind: "stock", product })}>
                              Add stock
                            </button>
                          )}
                          <button type="button" className="btn btn-secondary btn-sm" onClick={() => setDialog({ kind: "form", product })}>
                            <Icon name="edit" className="size-4" /> Edit
                          </button>
                          <button type="button" className="btn btn-danger btn-sm" onClick={() => void deactivate(product)} aria-label={`Hide ${product.name}`}>
                            Hide
                          </button>
                        </div>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
            <Pagination page={currentPage} totalPages={totalPages} onChange={setPage} />
          </>
        )}
      </div>

      {dialog?.kind === "form" && (
        <ProductFormDialog
          key={dialog.product?.productId ?? "new"}
          open
          product={dialog.product}
          userId={userId}
          onClose={() => setDialog(undefined)}
          onSaved={() => {
            setDialog(undefined);
            refresh();
          }}
        />
      )}
      {dialog?.kind === "stock" && (
        <StockDialog
          product={dialog.product}
          userId={userId}
          onClose={() => setDialog(undefined)}
          onSaved={() => {
            setDialog(undefined);
            stock.reload();
          }}
        />
      )}
    </div>
  );
}
