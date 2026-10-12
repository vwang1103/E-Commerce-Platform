export type ProductStatus = "DRAFT" | "ACTIVE" | "INACTIVE";

export interface CategoryDto {
  categoryId: number;
  parentCategoryId: number | null;
  name: string;
  slug: string;
}

export interface Product {
  productId: number;
  userId: string;
  categoryId: number;
  name: string;
  description: string;
  priceMinor: number;
  currency: string;
  status: ProductStatus;
  version: number;
  createdAt?: string;
  updatedAt?: string;
}

/** The fields shared by catalog rows and search hits. */
export type ProductSummary = Omit<Product, "createdAt" | "updatedAt">;

export interface ProductInput {
  categoryId: number;
  name: string;
  description: string;
  priceMinor: number;
  currency: string;
  status: ProductStatus;
}

export interface UpdateProductInput extends ProductInput {
  version: number;
}

export interface Page<T> {
  items: T[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
}

export interface ProductSearchPage extends Page<ProductSummary> {
  tookMillis: number;
}

export type OrderStatus =
  | "ACCEPTED"
  | "PENDING_PAYMENT"
  | "CONFIRMING_INVENTORY"
  | "INVENTORY_CONFIRMATION_FAILED"
  | "PAID"
  | "EXPIRED"
  | "REJECTED"
  | "FAILED";

export interface OrderItem {
  productId: number;
  productName: string;
  unitPriceMinor: number;
  quantity: number;
}

export interface Order {
  orderId: string;
  userId: string;
  status: OrderStatus;
  totalMinor: number;
  currency: string;
  paymentDeadline: string;
  version: number;
  createdAt: string;
  items: OrderItem[];
}

/** The catalog is the authority on name, price, and currency, so the request carries none of them. */
export interface CreateOrderRequest {
  items: Array<{ productId: number; quantity: number }>;
}

export interface Inventory {
  productId: number;
  totalQuantity: number;
  availableQuantity: number;
  reservedQuantity: number;
  soldQuantity: number;
  version: number;
  updatedAt?: string;
}

export type ReservationStatus = "TRY" | "CONFIRMED" | "CANCELLED";

export interface InventoryReservation {
  productId: number;
  quantity: number;
  status: ReservationStatus;
  expiresAt: string;
}

export interface ProblemDetail {
  title?: string;
  detail?: string;
  status?: number;
  instance?: string;
  errors?: Record<string, string>;
}
