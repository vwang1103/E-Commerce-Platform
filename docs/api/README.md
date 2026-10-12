# Explore the API with Swagger UI

From the repository root, start Docker Desktop and run:

```bash
./scripts/local.sh start all
```

Open **http://localhost:8085**. Swagger UI loads the single [OpenAPI contract](openapi.yaml) and sends requests to **http://localhost:8080**, the Spring Cloud Gateway. The storefront is at http://localhost:8084. `./scripts/local.sh start backend` also starts Swagger UI if you do not need the storefront. There is no embedded Swagger page in each Java service.

To try an endpoint, expand it, select **Try it out**, fill in any fields, then select **Execute**. Start with `GET /api/v1/categories` or `GET /api/v1/products`; neither needs a user ID. For seller or order endpoints:

1. Run `POST /api/v1/login` with a 2–40 character username, such as `shopper`, and copy the returned `userId` UUID. The same username returns the same UUID.
2. Paste that UUID into the endpoint's `X-User-Id` header field. This is a local demo identity, not authentication.
3. For order creation or payment, also enter a new `Idempotency-Key` value. Reuse the same key only when retrying the **same** request.

For checkout, first list products and choose one that is active, has stock, and is not owned by your demo user. The request body contains an `items` array, for example `{"items":[{"productId":1001,"quantity":1}]}`. To test a multi-item order, add another seeded product such as `1002`. Order creation returns `ACCEPTED`; use `GET /api/v1/orders/{orderId}` to watch it become `PENDING_PAYMENT`, then call `POST /api/v1/orders/{orderId}/payment` and poll until it becomes `PAID`. The order ID is returned as a string to preserve its 64-bit precision in JavaScript.

The Inventory Try/Confirm/Cancel endpoints are lower-level operations normally called by Order. A direct Try request needs an unused numeric `orderId`, the UUID `userId`, and a stocked `productId`. Confirm and Cancel are alternative terminal operations for that reservation; do not use both on the same order.

`openapi.yaml` is the only specification file. Swagger UI mounts it read-only; after changing the specification, restart the UI with `docker compose --env-file .env -f deploy/local/compose.yml restart swagger-ui`.
