# Storefront

Next.js 16 static storefront for the e-commerce MVP. It talks only to the public Spring Cloud Gateway API and is exported to `out/`. Local Compose serves it with Nginx; S3 and CloudFront hosting is planned, not deployed.

Source is organized by ownership: route entry points live in `src/app`, business UI and state live in
`src/features/<feature>`, and reusable presentation and technical utilities live in `src/shared`.

## Local development

Start the backend stack first, then:

```bash
cd frontend
npm ci
npm run dev
```

Open `http://localhost:3000`. In development, Next.js proxies `/api/*` to the gateway at `http://127.0.0.1:8080`, so no browser CORS configuration is required and API calls work with the same relative URLs used in production.

If the gateway runs somewhere else, override its server-side proxy target without exposing it to browser code:

```bash
GATEWAY_ORIGIN=http://localhost:9080 npm run dev
```

The Docker Compose storefront at `http://localhost:8084` uses the equivalent Nginx proxy.

## Verification

```bash
npm run typecheck
npm test
npm run build
```

## What the storefront covers

| Area | Route | Backend features used |
| --- | --- | --- |
| Home | `/` | Catalog list, categories |
| Browse | `/shop/` | Catalog list with category, price, and sort filters |
| Search | `/search/` and header suggestions | Elasticsearch search with relevance and timing |
| Product | `/product/?id=` | Product detail, live inventory counters |
| Cart | `/cart/`, slide-over drawer | Price and stock revalidation against Catalog and Inventory |
| Checkout | `/checkout/` | One idempotent multi-item order, live status polling, simulated payment |
| Orders | `/orders/`, `/order/?id=` | Customer order history, payment countdown, inventory reservation state |
| Seller console | `/seller/` | Product create/update with optimistic locking, opening stock |

The cart lives in `localStorage`. Checkout sends all cart lines as one multi-product order with one stable idempotency key.

Order timestamps from the backend carry no UTC offset; the client parses them as UTC so payment countdowns are correct in every time zone.

The MVP uses a deliberately simple username-only login. The gateway returns a stable UUID, the browser stores the `{userId, username}` identity locally, and API calls send the UUID through `X-User-Id`. The UUID is never displayed or editable in the storefront. One identity can both buy and sell, but the Order service rejects attempts to buy a product owned by that same user. This is a local identity mechanism, not production authentication.

Styling uses Tailwind CSS v4. Development runs with Turbopack; production export uses webpack because its build is deterministic in restricted CI and container environments.

