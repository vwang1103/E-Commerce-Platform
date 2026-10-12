# Architecture

## System context

```text
Browser
   |
   v
CloudFront + AWS WAF
   |-- static routes ----------> S3 (Next.js static output)
   `-- /api/* -----------------> ALB
                                  |
                                  v
                         EC2 Auto Scaling Group
                         +-----------------------+
                         | Spring Cloud Gateway  |
                         | Catalog service       |
                         | Inventory service     |
                         | Order service         |
                         | Consul agent          |
                         +-----------------------+
                             |       |       |
                +------------+       |       +----------------+
                v                    v                        v
       MySQL core/order shards   Redis TCC                Kafka
                |                                             |
                +-- catalog outbox -> product events ---------+
                                                              |
                                                              v
                                                       Elasticsearch
```

CloudFront is the AWS public front door. Static Next.js output comes from a private S3 origin, while `/api/*` is forwarded without caching to the internal ALB and Spring Cloud Gateway. The ALB security group accepts HTTP only from AWS's managed CloudFront origin-facing prefix list. AWS WAF provides coarse edge protection. Gateway applies route-specific Redis token-bucket limits keyed by the supplied demo user UUID when present and otherwise by client IP.

The same application boundaries run locally in Docker Compose with Nginx in place of S3/CloudFront and one instance of each Spring service. `deploy/aws` now contains runnable TypeScript CDK stacks and clone-to-account deployment scripts. Both configurations synthesize CloudFormation successfully; the repository has not deployed them to a live AWS account and does not claim live availability evidence.

## Service boundaries

| Service | Owns | Does not own |
| --- | --- | --- |
| Gateway | Public routing, username-only demo login, correlation IDs, CORS, and Redis-backed rate limits | Product, stock, or order business rules |
| Catalog | Categories, products, seller ownership, prices, product outbox, and search projection | Inventory quantities or order lifecycle |
| Inventory | Durable stock, multi-item reservations, Redis Lua Try/Confirm/Cancel, and Redis reconciliation | Product descriptions, prices, or order state |
| Order | Multi-item checkout, idempotency, sharding keys, Kafka workflow, payment simulation, and expiration | Product catalog data or direct stock mutation |

Each Spring service is independently deployable and registers with Consul. Gateway routes to service names, and Order calls Catalog and Inventory through OpenFeign with Spring Cloud LoadBalancer. Services communicate through HTTP contracts or versioned Kafka events and never read another service's tables. `service-common` is a shared technical library, not a deployable service.

The local username endpoint returns a deterministic UUID carried in `X-User-Id`. It supports the demo's buyer/seller ownership rules but is not authentication or authorization.

## Flash-sale order flow

```text
POST /api/v1/orders (X-User-Id + Idempotency-Key + items[])
  -> Gateway order-write rate limit
  -> Order loads current product names/prices from Catalog
  -> reject inactive products, self-purchase, or mixed currencies
  -> Inventory Redis Lua TRY holds every item
  -> Inventory persists durable TRY reservations
  -> Order commits ACCEPTED order + items + order.requested outbox
  -> return 202 Accepted

Order outbox + Kafka
  -> publish order.requested keyed by order ID
  -> deduplicate by event ID
  -> transition ACCEPTED to PENDING_PAYMENT

Payment before deadline
  -> transition to CONFIRMING_INVENTORY once
  -> write inventory.confirm to the outbox
  -> Kafka consumer calls Inventory Confirm
  -> transition to PAID

Expiration scheduler
  -> claim overdue orders with database locking
  -> transition PENDING_PAYMENT to EXPIRED
  -> write inventory.cancel to the outbox
  -> Kafka consumer calls Inventory Cancel
```

Order creation is idempotent per user and `Idempotency-Key`; retrying the same request returns the committed order. Kafka uses order ID as the partition key and is treated as at-least-once delivery. Event IDs, processed-event records, unique constraints, and idempotent state transitions make duplicates safe.

Transient consumer failures are retried and then written to a `.DLT` topic. Exhausted Inventory Confirm retries place the order in `INVENTORY_CONFIRMATION_FAILED` instead of incorrectly reporting it as paid. Replaying the same command can complete the transition. Internal replay endpoints are available only on direct service ports and are not routed by Gateway.

## Inventory consistency

Redis Lua provides the fast atomic stock hold:

- `TRY` rejects missing or insufficient stock, otherwise decreases Redis availability and creates an order/product reservation.
- `CONFIRM` makes a TRY reservation terminal without decrementing stock a second time.
- `CANCEL` restores stock only for a TRY reservation and makes it terminal.

An order may reserve multiple products. Each Redis key contains a product hash tag such as `inventory:{productId}`, keeping a single reservation operation in one Redis Cluster slot. Repeating the same operation is idempotent.

Try holds Redis first and then persists the durable MySQL reservation. If persistence fails, the service attempts to cancel every new Redis hold before returning the error. Confirm and Cancel take the opposite approach: they commit the MySQL stock transition and a durable Redis-sync task in one transaction. A scheduler applies the corresponding idempotent Lua operation afterward. Redis downtime can delay cache convergence but cannot erase the committed MySQL transition.

MySQL is the durable source for stock, reservation status, and reconciliation. Separate schedulers cancel expired reservations, retry pending Redis-sync tasks, and report MySQL/Redis availability drift.

## Search consistency

Catalog commits the product change and a `catalog_outbox` row in the same MySQL transaction. The outbox relay publishes a versioned envelope to `catalog.product.v1`, keyed by product ID. The Kafka consumer writes the Elasticsearch document with external versioning, so a replayed older product event cannot overwrite a newer version.

Search queries Elasticsearch across analyzed product name and description fields and use relevance plus product ID for stable pagination. This makes short fuzzy input such as `sk` rank matching names such as “Sketch Smart Stylus” and “Skyline 1000-Piece Puzzle” near the top. Search is eventually consistent; direct product lookup remains backed by Catalog's MySQL data.

An internal MySQL `LIKE` adapter exists only as a future benchmark baseline. It is not routed through Gateway or used by the storefront. No search-performance percentage should be claimed until the same dataset and query corpus are measured and saved.

## Order sharding

Only the growing order dataset is sharded. Catalog and Inventory remain unsharded.

- Database shard: a stable hash of the UUID `user_id` chooses `order_ds_0` or `order_ds_1`.
- Table shard: `order_id % 2` chooses `orders_0` or `orders_1` and the matching `order_items` table.
- IDs: `SnowflakeOrderIdGenerator` creates sortable 64-bit order and order-item IDs.
- Queries: order APIs require both user ID and order ID where applicable so ShardingSphere can target one database instead of broadcasting.
- Tooling: ShardingSphere-JDBC is configured programmatically, and Flyway applies the same migration history to both physical databases.

Integration tests verify UUID database routing, physical table placement, and Snowflake ID behavior.

## AWS availability model

The TypeScript CDK application uses one fixed configuration. It generates four CloudFormation stacks
for networking, data, compute, and edge delivery. Two EC2 application nodes run across two
Availability Zones so the project demonstrates load balancing and instance replacement.
The data services use small, mostly single-node settings to keep the example understandable; the
AWS deployment is not a production high-availability data tier.

Each EC2 node runs Gateway, Catalog, Inventory, Order, and a Consul server agent. An internal ALB
health-checks Gateway; EC2 Auto Scaling replaces instances that fail EC2 or ALB health checks and
scales on CPU usage. RDS, ElastiCache, MSK, OpenSearch, S3, and Secrets Manager remain outside the
Auto Scaling Group as managed services. CloudWatch collects container logs. See the
[AWS deployment guide](../../deploy/aws/README.md) for account setup, costs, synthesis, and
deployment commands.
