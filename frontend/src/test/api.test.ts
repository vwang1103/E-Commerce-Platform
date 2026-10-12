import { afterEach, describe, expect, it, vi } from "vitest";
import { ApiError, api } from "@/shared/lib/api";

const USER_ID = "aaaaaaaa-aaaa-4aaa-8aaa-aaaaaaaaaaaa";

describe("API client", () => {
  afterEach(() => vi.unstubAllGlobals());

  it("creates the local identity through the gateway login endpoint", async () => {
    const fetchMock = vi.fn().mockResolvedValue(
      new Response(JSON.stringify({ userId: USER_ID, username: "shopper" }), {
        status: 200,
        headers: { "Content-Type": "application/json" },
      }),
    );
    vi.stubGlobal("fetch", fetchMock);

    await expect(api.login("shopper")).resolves.toEqual({ userId: USER_ID, username: "shopper" });
    expect(fetchMock).toHaveBeenCalledWith(
      "/api/v1/login",
      expect.objectContaining({ method: "POST", body: JSON.stringify({ username: "shopper" }) }),
    );
  });

  it("sends checkout identity and idempotency headers", async () => {
    const fetchMock = vi.fn().mockResolvedValue(
      new Response(JSON.stringify({ orderId: "102643349604794368" }), {
        status: 202,
        headers: { "Content-Type": "application/json" },
      }),
    );
    vi.stubGlobal("fetch", fetchMock);

    await api.createOrder(
      { items: [{ productId: 42, quantity: 1 }] },
      USER_ID,
      "checkout-fixed-key",
    );

    expect(fetchMock).toHaveBeenCalledWith(
      "/api/v1/orders",
      expect.objectContaining({
        method: "POST",
        headers: expect.objectContaining({ "Idempotency-Key": "checkout-fixed-key", "X-User-Id": USER_ID }),
      }),
    );
  });

  it("maps problem details to an ApiError", async () => {
    vi.stubGlobal(
      "fetch",
      vi.fn().mockResolvedValue(
        new Response(JSON.stringify({ title: "Conflict", detail: "Insufficient inventory" }), {
          status: 409,
          headers: { "Content-Type": "application/problem+json" },
        }),
      ),
    );

    await expect(api.getProduct(99)).rejects.toEqual(
      expect.objectContaining({ name: "ApiError", status: 409, message: "Insufficient inventory" }),
    );
  });

  it("explains rate limiting in plain language", async () => {
    vi.stubGlobal("fetch", vi.fn().mockResolvedValue(new Response("{}", { status: 429 })));
    const failure = await api.getProduct(1).catch((reason: unknown) => reason);
    expect(failure).toBeInstanceOf(ApiError);
    expect((failure as ApiError).message).toMatch(/wait a moment/i);
  });

  it("loads the signed-in user's products without exposing the UUID in the URL", async () => {
    const fetchMock = vi.fn().mockResolvedValue(
      new Response(JSON.stringify({ items: [], page: 0, size: 100, totalElements: 0, totalPages: 0 }), { status: 200 }),
    );
    vi.stubGlobal("fetch", fetchMock);
    await api.listMyProducts(USER_ID);
    expect(fetchMock.mock.calls[0][0]).toBe("/api/v1/products/mine?page=0&size=100");
    expect(fetchMock.mock.calls[0][1]).toEqual(
      expect.objectContaining({ headers: expect.objectContaining({ "X-User-Id": USER_ID }) }),
    );
  });

  it("follows catalog pages until all are loaded", async () => {
    const page = (index: number) =>
      new Response(JSON.stringify({ items: [{ productId: index }], page: index, size: 100, totalElements: 2, totalPages: 2 }), { status: 200 });
    const fetchMock = vi.fn().mockResolvedValueOnce(page(0)).mockResolvedValueOnce(page(1));
    vi.stubGlobal("fetch", fetchMock);
    const all = await api.listAllProducts();
    expect(all.map((product) => product.productId)).toEqual([0, 1]);
    expect(fetchMock).toHaveBeenCalledTimes(2);
  });
});
