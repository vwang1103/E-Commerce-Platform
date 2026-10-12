"use client";

import { useState, type FormEvent } from "react";
import { Spinner } from "@/shared/components/Feedback";
import { Modal } from "@/shared/components/Modal";
import { useCategories } from "@/features/catalog/hooks/useCategories";
import { api, toError } from "@/shared/lib/api";
import { invalidateCatalog } from "@/features/catalog/lib/catalog";
import { invalidateStock } from "@/features/inventory/lib/stock";
import {
  EMPTY_FORM,
  formFromProduct,
  toProductInput,
  validateProductForm,
  type ProductFormErrors,
  type ProductFormValues,
} from "@/features/seller/lib/seller";
import { toast } from "@/shared/lib/toast";
import type { Product } from "@/shared/lib/types";

function Field({ label, error, children, htmlFor }: Readonly<{ label: string; error?: string; children: React.ReactNode; htmlFor: string }>) {
  return (
    <div>
      <label htmlFor={htmlFor} className="label">
        {label}
      </label>
      {children}
      {error && (
        <p id={`${htmlFor}-error`} className="field-error">
          {error}
        </p>
      )}
    </div>
  );
}

/** Creates a product (optionally with opening stock) or edits an existing one. */
export function ProductFormDialog({
  open,
  product,
  userId,
  onClose,
  onSaved,
}: Readonly<{ open: boolean; product?: Product; userId: string; onClose: () => void; onSaved: () => void }>) {
  const mode = product ? "edit" : "create";
  const { categories } = useCategories();
  const [values, setValues] = useState<ProductFormValues>(() => (product ? formFromProduct(product) : EMPTY_FORM));
  const [errors, setErrors] = useState<ProductFormErrors>({});
  const [submitting, setSubmitting] = useState(false);
  const [failure, setFailure] = useState<Error>();

  function set<K extends keyof ProductFormValues>(key: K, value: ProductFormValues[K]) {
    setValues((current) => ({ ...current, [key]: value }));
    setErrors((current) => ({ ...current, [key]: undefined }));
  }

  function bind(key: keyof ProductFormValues) {
    return {
      id: `product-${key}`,
      "aria-invalid": errors[key] ? true : undefined,
      "aria-describedby": errors[key] ? `product-${key}-error` : undefined,
    };
  }

  async function submit(event: FormEvent) {
    event.preventDefault();
    const found = validateProductForm(values, mode);
    setErrors(found);
    if (Object.values(found).some(Boolean)) return;

    setSubmitting(true);
    setFailure(undefined);
    try {
      const input = toProductInput(values);
      if (product) {
        await api.updateProduct(product.productId, { ...input, version: product.version }, userId);
        toast("Product updated");
      } else {
        const created = await api.createProduct(input, userId);
        if (values.stock.trim() !== "") {
          try {
            await api.createInventory(created.productId, Number(values.stock), userId);
            toast("Product created and stocked");
          } catch (reason) {
            toast(`Product created, but stock could not be set: ${toError(reason).message}`, "error");
          }
        } else {
          toast("Product created without stock");
        }
      }
      invalidateCatalog();
      invalidateStock();
      onSaved();
    } catch (reason) {
      setFailure(toError(reason));
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <Modal open={open} onClose={onClose} title={product ? `Edit ${product.name}` : "New product"} wide>
      <form onSubmit={(event) => void submit(event)} noValidate className="grid gap-4 sm:grid-cols-2">
        <div className="sm:col-span-2">
          <Field label="Name" htmlFor="product-name" error={errors.name}>
            <input className="input" maxLength={255} {...bind("name")} value={values.name} onChange={(event) => set("name", event.target.value)} />
          </Field>
        </div>
        <Field label="Category" htmlFor="product-categoryId">
          <select className="input" {...bind("categoryId")} value={values.categoryId} onChange={(event) => set("categoryId", event.target.value)}>
            {categories.map((category) => (
              <option key={category.id} value={category.id}>
                {category.name}
              </option>
            ))}
          </select>
        </Field>
        <div className="sm:col-span-2">
          <Field label="Description" htmlFor="product-description" error={errors.description}>
            <textarea
              className="input min-h-28"
              maxLength={5000}
              {...bind("description")}
              value={values.description}
              onChange={(event) => set("description", event.target.value)}
            />
          </Field>
        </div>
        <Field label="Price" htmlFor="product-price" error={errors.price}>
          <input className="input" inputMode="decimal" placeholder="19.99" {...bind("price")} value={values.price} onChange={(event) => set("price", event.target.value)} />
        </Field>
        <Field label="Currency" htmlFor="product-currency" error={errors.currency}>
          <input className="input uppercase" maxLength={3} {...bind("currency")} value={values.currency} onChange={(event) => set("currency", event.target.value)} />
        </Field>
        <Field label="Status" htmlFor="product-status">
          <select
            className="input"
            {...bind("status")}
            value={values.status}
            onChange={(event) => set("status", event.target.value as ProductFormValues["status"])}
          >
            <option value="ACTIVE">Active (visible in the store)</option>
            <option value="DRAFT">Draft</option>
            <option value="INACTIVE">Inactive (hidden)</option>
          </select>
        </Field>
        {mode === "create" && (
          <Field label="Opening stock" htmlFor="product-stock" error={errors.stock}>
            <input className="input" inputMode="numeric" placeholder="10" {...bind("stock")} value={values.stock} onChange={(event) => set("stock", event.target.value)} />
          </Field>
        )}

        {failure && (
          <p role="alert" className="rounded-xl bg-rose-50 p-3 text-sm text-rose-800 sm:col-span-2">
            {failure.message}
          </p>
        )}
        {mode === "edit" && (
          <p className="text-xs text-slate-500 sm:col-span-2">
            Inactive and draft products disappear from this list and from the storefront.
          </p>
        )}

        <div className="flex justify-end gap-3 sm:col-span-2">
          <button type="button" className="btn btn-secondary" onClick={onClose}>
            Cancel
          </button>
          <button type="submit" className="btn btn-primary" disabled={submitting}>
            {submitting ? (
              <>
                <Spinner className="size-4" /> Saving…
              </>
            ) : mode === "edit" ? (
              "Save changes"
            ) : (
              "Create product"
            )}
          </button>
        </div>
      </form>
    </Modal>
  );
}
