# Run the project locally

The complete containerized application needs Docker Desktop with Docker Compose, Bash, and curl. Python 3 is additionally used by the smoke and demo-order scripts. Java 21 and Maven 3.9+ are needed only for host-side backend development; Node.js 24.11.0 and npm 11.6.1 are needed only for host-side frontend development.

Application and container versions are pinned in `pom.xml`, `frontend/package-lock.json`, `deploy/aws/package-lock.json`, and `.env.example`.

From the repository root, start Docker Desktop and run:

```bash
./scripts/local.sh start all
```

On the first run, the script copies `.env.example` to `.env`, builds the backend and storefront images, starts dependencies, applies Flyway migrations and local demo seeds, and waits for health checks.

Open the storefront at http://localhost:8084, [Swagger UI](api/README.md) at http://localhost:8085, Gateway at http://localhost:8080, or the Consul UI at http://localhost:8500.

For a live Next.js development server at http://localhost:3000 with the backend in Docker:

```bash
./scripts/local.sh dev
```

`dev` runs Next.js in the foreground. Ctrl+C stops Next.js; the Docker backend continues until `./scripts/local.sh stop` is run.

Other useful commands:

```bash
./scripts/local.sh start backend   # backend, infrastructure, and Swagger only
./scripts/local.sh status
./scripts/local.sh logs
./scripts/local.sh logs order-service
./scripts/local.sh smoke           # complete browser-facing buyer workflow
./scripts/local.sh seed-orders     # paid history for demo-shopper
./scripts/local.sh stop            # stop containers and retain volumes
```

Use `--skip-tests` with `start` or `dev` to skip backend tests during an image build, or `--no-build` to reuse existing images. `dev --skip-install` reuses `frontend/node_modules`. `start all --smoke` runs the buyer workflow after startup.

Run the complete backend verification suite from the repository root:

```bash
mvn clean verify
```

This includes Docker-backed Testcontainers integration tests. Use `mvn spotless:apply` to fix Java formatting.

Run frontend verification directly:

```bash
cd frontend
npm ci
npm run typecheck
npm test
npm run build
```

Compose loads readable demo products and matching stock from service-owned seed directories; tests and non-Compose migrations load schema only. The username-only login returns a deterministic UUID sent as `X-User-Id`. It has no password, session validation, or authorization and is not production authentication. `seed-orders` uses `demo-shopper` by default; set `SEED_USERNAME` to use a different identity.

Swagger UI loads the checked-in [OpenAPI contract](api/openapi.yaml). Internal DLT replay endpoints are available on direct service ports for local recovery testing and are deliberately absent from Gateway.

To intentionally erase every local database, Kafka log, Redis entry, Elasticsearch index, and Consul volume (ordinary `stop` retains them):

```bash
docker compose --env-file .env -f deploy/local/compose.yml down --volumes
```

The next start recreates the schemas and reloads demo data. For runtime behavior, see [Architecture](architecture/system-overview.md). For container and migration ownership, see [Reproducible local infrastructure](../deploy/local/README.md). To provision the same application boundaries on EC2 with CloudFront, WAF, managed data services, and CloudFormation, follow the separate [AWS deployment guide](../deploy/aws/README.md).
