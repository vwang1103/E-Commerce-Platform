import type {
  CategoryDto,
  CreateOrderRequest,
  Inventory,
  InventoryReservation,
  Order,
  Page,
  ProblemDetail,
  Product,
  ProductInput,
  ProductSearchPage,
  UpdateProductInput,
} from "./types";
import type { UserIdentity } from "./user";

const configuredBaseUrl = process.env.NEXT_PUBLIC_API_BASE_URL?.replace(/\/$/, "") ?? "";

export class ApiError extends Error {
  constructor(
    public readonly status: number,
    message: string,
    public readonly problem?: ProblemDetail,
  ) {
    super(message);
    this.name = "ApiError";
  }
}

async function request<T>(path: string, init?: RequestInit): Promise<T> {
  const response = await fetch(`${configuredBaseUrl}${path}`, {
    ...init,
    headers: {
      Accept: "application/json",
      ...init?.headers,
    },
  });

  if (!response.ok) {
    const problem = (await response.json().catch(() => undefined)) as ProblemDetail | undefined;
    const message =
      response.status === 429
        ? "We're receiving a lot of requests. Please wait a moment and try again."
        : (problem?.detail ?? problem?.title ?? `Request failed (${response.status})`);
    throw new ApiError(response.status, message, problem);
  }

  return response.json() as Promise<T>;
}

const jsonHeaders = { "Content-Type": "application/json" };

/** The catalog list endpoint caps pages at 100 items. */
const CATALOG_PAGE_SIZE = 100;
const CATALOG_MAX_PAGES = 5;

function listProducts(page = 0, size = 12) {
  const params = new URLSearchParams({ page: String(page), size: String(size) });
  return request<Page<Product>>(`/api/v1/products?${params}`);
}

async function listAllProducts(): Promise<Product[]> {
  const first = await listProducts(0, CATALOG_PAGE_SIZE);
  const items = [...first.items];
  const lastPage = Math.min(first.totalPages, CATALOG_MAX_PAGES);
  for (let page = 1; page < lastPage; page++) {
    items.push(...(await listProducts(page, CATALOG_PAGE_SIZE)).items);
  }
  return items;
}

export const api = {
  login(username: string) {
    return request<UserIdentity>("/api/v1/login", {
      method: "POST",
      headers: jsonHeaders,
      body: JSON.stringify({ username }),
    });
  },

  listProducts,
  listAllProducts,

  listCategories() {
    return request<CategoryDto[]>("/api/v1/categories");
  },

  getProduct(productId: number) {
    return request<Product>(`/api/v1/products/${productId}`);
  },

  listMyProducts(userId: string, page = 0, size = 100) {
    const params = new URLSearchParams({ page: String(page), size: String(size) });
    return request<Page<Product>>(`/api/v1/products/mine?${params}`, { headers: { "X-User-Id": userId } });
  },

  createProduct(input: ProductInput, userId: string) {
    return request<Product>("/api/v1/products", {
      method: "POST",
      headers: { ...jsonHeaders, "X-User-Id": userId },
      body: JSON.stringify(input),
    });
  },

  updateProduct(productId: number, input: UpdateProductInput, userId: string) {
    return request<Product>(`/api/v1/products/${productId}`, {
      method: "PUT",
      headers: { ...jsonHeaders, "X-User-Id": userId },
      body: JSON.stringify(input),
    });
  },

  searchProducts(query: string, page = 0, size = 12) {
    const params = new URLSearchParams({ q: query, page: String(page), size: String(size) });
    return request<ProductSearchPage>(`/api/v1/search?${params}`);
  },

  getInventory(productId: number) {
    return request<Inventory>(`/api/v1/inventory/${productId}`);
  },

  listInventory(productIds: readonly number[]) {
    const params = new URLSearchParams({ productIds: productIds.join(",") });
    return request<Inventory[]>(`/api/v1/inventory?${params}`);
  },

  createInventory(productId: number, totalQuantity: number, userId: string) {
    return request<Inventory>(`/api/v1/inventory/${productId}`, {
      method: "PUT",
      headers: { ...jsonHeaders, "X-User-Id": userId },
      body: JSON.stringify({ totalQuantity }),
    });
  },

  getReservation(orderId: string) {
    return request<InventoryReservation>(`/api/v1/inventory/reservations/${orderId}`);
  },

  createOrder(order: CreateOrderRequest, userId: string, idempotencyKey: string) {
    return request<Order>("/api/v1/orders", {
      method: "POST",
      headers: {
        ...jsonHeaders,
        "Idempotency-Key": idempotencyKey,
        "X-User-Id": userId,
      },
      body: JSON.stringify(order),
    });
  },

  getOrder(orderId: string, userId: string) {
    return request<Order>(`/api/v1/orders/${orderId}`, {
      headers: { "X-User-Id": String(userId) },
    });
  },

  listOrders(userId: string, page = 0, size = 10) {
    return request<Page<Order>>(`/api/v1/orders?page=${page}&size=${size}`, {
      headers: { "X-User-Id": userId },
    });
  },

  payOrder(orderId: string, userId: string, idempotencyKey: string) {
    return request<Order>(`/api/v1/orders/${orderId}/payment`, {
      method: "POST",
      headers: {
        "Idempotency-Key": idempotencyKey,
        "X-User-Id": userId,
      },
    });
  },
};

export function newIdempotencyKey(prefix: string): string {
  return `${prefix}-${crypto.randomUUID()}`;
}

export function isNotFound(reason: unknown): boolean {
  return reason instanceof ApiError && reason.status === 404;
}

export function toError(reason: unknown, fallback = "Something went wrong. Please try again."): Error {
  return reason instanceof Error ? reason : new Error(fallback);
}
