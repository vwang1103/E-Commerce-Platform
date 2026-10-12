# Event contracts

Catalog writes an unpublished row to `catalog_outbox` in the same MySQL transaction as each product change. When `CATALOG_EVENTS_ENABLED=true`, the outbox publisher claims rows in small ordered batches, publishes them to `catalog.product.v1` keyed by product ID, and only then records `published_at`. A crash can therefore produce a duplicate event, but cannot silently lose a committed product change.

The Elasticsearch consumer handles those at-least-once deliveries with external versioning. Product version `0` maps to Elasticsearch external version `1`, version `1` maps to `2`, and so on. Replayed or older events cannot replace newer product state.

The publisher wraps each outbox payload in this versioned envelope:

```json
{
  "eventId": "1a50742a-cc55-4f25-a264-6fd768b29ad7",
  "eventType": "product.updated",
  "occurredAt": "2026-10-10T20:10:00Z",
  "traceId": "8c5853fe2c74461e",
  "aggregateType": "product",
  "aggregateId": "42",
  "schemaVersion": 1,
  "payload": {
    "productId": 42,
    "userId": "0337c503-6233-385f-ab7b-0a4c48e29513",
    "categoryId": 100,
    "name": "Developer Laptop",
    "priceMinor": 129900,
    "currency": "USD",
    "status": "ACTIVE",
    "version": 1
  }
}
```

Consumers must deduplicate by `eventId`. Schema changes are additive within a version; breaking changes require a new `schemaVersion` and a transition period that supports both versions.

## Order workflow

Order submission reserves every requested product as one inventory operation, then commits the `ACCEPTED` order, all order items, and the `order.requested` outbox row in one MySQL transaction. The outbox publisher routes workflow events and inventory commands by order ID, preserving one ordered event stream for the complete multi-item order.

The workflow consumer deduplicates with `(consumer_name, event_id)` and advances `ACCEPTED` to `PENDING_PAYMENT`. Payment changes the order to `CONFIRMING_INVENTORY` and writes `inventory.confirm`; successful asynchronous confirmation then changes it to `PAID`. If retries are exhausted, the DLT listener records `INVENTORY_CONFIRMATION_FAILED`; replaying the same DLT command can recover that order to `PAID`. The database-time expiry scheduler claims overdue orders with `FOR UPDATE SKIP LOCKED`, changes them to `EXPIRED`, and writes `inventory.cancel`. Inventory command handling is also deduplicated, and the downstream batch Confirm/Cancel APIs are independently idempotent.

Inventory Confirm and Cancel commit the MySQL stock transition and a durable Redis synchronization task in the same transaction. A scheduler applies that task with idempotent Lua. Redis being unavailable therefore delays cache convergence without losing the authoritative stock transition or allowing Redis success followed by an unrecorded MySQL failure.

Transient consumer failures use exponential retry and then publish the original record to `<source-topic>.DLT`. The shared recovery component preserves the failed record's original-topic header and exposes a controlled service-local replay operation. Replay republishes the same key and JSON envelope to the original topic and strips DLT transport headers; the stable `eventId` therefore continues to protect idempotent consumers.

The gateway deliberately does not route `/internal/**`. In local Compose, replay a reviewed Order DLT record directly against the Order service:

```bash
curl --request POST http://localhost:8083/internal/v1/events/dlt/replay \
  --header 'Content-Type: application/json' \
  --data '{"topic":"inventory.command.v1.DLT","partition":0,"offset":0}'
```

Catalog uses the same endpoint on port `8081` and permits only `catalog.product.v1.DLT`; Order permits only `order.workflow.v1.DLT` and `inventory.command.v1.DLT`. The service rejects a DLT whose original-topic metadata does not match the configured allowlist. The current local demo has no operator authentication; never expose these direct service ports publicly. A production deployment must put replay behind a private, authenticated operator network.

`OrderKafkaWorkflowIntegrationTest` proves the whole failure path with a real Kafka container: exhaust retries, publish to `.DLT`, fetch by topic/partition/offset, replay, and successfully invoke the business consumer. Consumers ignore an event already committed in `processed_event`; delete deduplication records only during an explicitly controlled recovery after verifying the business transition did not commit.
