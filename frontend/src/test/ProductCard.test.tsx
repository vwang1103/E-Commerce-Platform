import { render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { afterEach, describe, expect, it, vi } from "vitest";
import { api } from "@/shared/lib/api";
import { cartActions, getCartLines } from "@/features/cart/hooks/useCart";
import { ProductCard } from "@/features/catalog/components/ProductCard";

const product = {
  productId: 42,
  userId: "aaaaaaaa-aaaa-4aaa-8aaa-aaaaaaaaaaaa",
  categoryId: 100,
  name: "Developer Laptop",
  description: "A dependable workstation",
  priceMinor: 129900,
  currency: "USD",
  status: "ACTIVE" as const,
  version: 0,
};

describe("ProductCard", () => {
  afterEach(() => {
    cartActions.clear();
    vi.restoreAllMocks();
  });

  it("renders product information and a detail link", async () => {
    vi.spyOn(api, "listCategories").mockResolvedValue([{ categoryId: 100, parentCategoryId: null, name: "Electronics", slug: "electronics" }]);
    render(<ProductCard product={product} />);

    expect(screen.getByRole("heading", { name: "Developer Laptop" })).toBeInTheDocument();
    expect(screen.getByText("$1,299.00")).toBeInTheDocument();
    expect(await screen.findByText("Electronics")).toBeInTheDocument();
    // The trailing slash is added by the exported-site config, not by the link component itself.
    expect(screen.getAllByRole("link", { name: /developer laptop/i })[0].getAttribute("href")).toMatch(/^\/product\/?\?id=42$/);
  });

  it("adds one unit to the cart and opens the drawer", async () => {
    render(<ProductCard product={product} />);
    await userEvent.click(screen.getByRole("button", { name: "Add Developer Laptop to cart" }));
    await userEvent.click(screen.getByRole("button", { name: "Add Developer Laptop to cart" }));

    expect(getCartLines()).toEqual([expect.objectContaining({ productId: 42, quantity: 2, unitPriceMinor: 129900 })]);
  });

  it("disables purchase for products that are not active", () => {
    render(<ProductCard product={{ ...product, status: "INACTIVE" }} />);
    expect(screen.getByRole("button", { name: "Add Developer Laptop to cart" })).toBeDisabled();
  });
});
