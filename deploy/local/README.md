# Reproducible local infrastructure

`compose.yml` runs the complete local platform:

- Gateway, Catalog, Inventory, and Order as separate health-checked, non-root Java 21 containers
- Next.js 16 static output served by unprivileged Nginx with a same-origin `/api` Gateway proxy
- Swagger UI serving the mounted `docs/api/openapi.yaml` contract
- Consul in development mode for service registration and discovery
- single-node Apache Kafka in KRaft mode
- one shared Redis server with append-only persistence
- single-node Elasticsearch
- one core MySQL server containing separate Catalog and Inventory databases
- two independent MySQL order shards
- five one-shot Flyway migration jobs

The backend services share one locally built image containing four independently launched JARs. The frontend has a separate multi-stage image: Node creates the static Next.js export and unprivileged Nginx serves it. Image versions and default ports are pinned in the root `.env.example`.

Host ports bind to `127.0.0.1`, while inter-container traffic stays on the project-scoped `ecommerce-private` bridge network. Stateful services use named volumes. Health checks gate migrations and application startup; a Flyway container remaining in the successfully exited state is expected.

Compose enables the Catalog and Order Kafka pipelines, Elasticsearch indexing, Inventory Redis synchronization, and direct service-local DLT replay. It runs one instance of each Spring service, so this environment does not prove multi-instance availability.

All startup, development, logging, testing, shutdown, and reset commands live in the single [local runbook](../../docs/getting-started.md).

## Migration ownership

| Migration directory | Target |
| --- | --- |
| `deploy/local/migrations/core` | Creates `catalog_db` and `inventory_db`, then grants the local application user access |
| `backend/catalog-service/src/main/resources/db/migration` | Catalog schema, base categories, products, and transactional outbox |
| `backend/catalog-service/src/main/resources/db/seed` | Compose-only demo categories, products, and search-index outbox events |
| `backend/inventory-service/src/main/resources/db/migration` | Durable inventory, reservations, and Redis synchronization tasks |
| `backend/inventory-service/src/main/resources/db/seed` | Compose-only stock for demo products |
| `backend/order-service/src/main/resources/db/migration` | Applied independently to both order shards; creates both physical table suffixes, processed events, and the transactional outbox |

Each business service owns one canonical migration history under its backend module. The application, Testcontainers tests, and Docker Compose consume those same files, preventing duplicate migration copies from drifting. The only infrastructure-owned migration is the core bootstrap because it creates databases and grants permissions before a service database exists.
