import type { Product, ProductInput, ProductStatus } from "@/shared/lib/types";

export interface ProductFormValues {
  name: string;
  categoryId: string;
  description: string;
  price: string;
  currency: string;
  status: ProductStatus;
  stock: string;
}

export type ProductFormErrors = Partial<Record<keyof ProductFormValues, string>>;

export const EMPTY_FORM: ProductFormValues = {
  name: "",
  categoryId: "100",
  description: "",
  price: "",
  currency: "USD",
  status: "ACTIVE",
  stock: "10",
};

export function formFromProduct(product: Product): ProductFormValues {
  return {
    name: product.name,
    categoryId: String(product.categoryId),
    description: product.description,
    price: minorToInput(product.priceMinor),
    currency: product.currency,
    status: product.status,
    stock: "",
  };
}

/** Parses a decimal price such as "12.5" into minor units, or returns undefined. */
export function parsePriceToMinor(text: string): number | undefined {
  const trimmed = text.trim();
  if (!/^\d{1,9}(\.\d{1,2})?$/.test(trimmed)) return undefined;
  return Math.round(Number(trimmed) * 100);
}

export function minorToInput(minor: number): string {
  return (minor / 100).toFixed(2);
}

export function validateProductForm(values: ProductFormValues, mode: "create" | "edit"): ProductFormErrors {
  const errors: ProductFormErrors = {};
  if (!values.name.trim()) errors.name = "Enter a product name.";
  else if (values.name.trim().length > 255) errors.name = "Keep the name under 256 characters.";

  if (!values.description.trim()) errors.description = "Enter a description.";
  else if (values.description.trim().length > 5000) errors.description = "Keep the description under 5,001 characters.";

  if (parsePriceToMinor(values.price) === undefined) errors.price = "Enter a price like 19.99.";
  if (!/^[A-Za-z]{3}$/.test(values.currency.trim())) errors.currency = "Use a 3-letter currency code.";

  if (mode === "create" && values.stock.trim() !== "" && !/^\d{1,9}$/.test(values.stock.trim())) {
    errors.stock = "Enter a whole number, or leave blank to skip.";
  }
  return errors;
}

export function toProductInput(values: ProductFormValues): ProductInput {
  return {
    categoryId: Number(values.categoryId),
    name: values.name.trim(),
    description: values.description.trim(),
    priceMinor: parsePriceToMinor(values.price) ?? 0,
    currency: values.currency.trim().toUpperCase(),
    status: values.status,
  };
}
