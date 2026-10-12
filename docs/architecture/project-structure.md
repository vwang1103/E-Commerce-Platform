# Project structure

## Repository map

```text
ecommerce-platform/
├── backend/
│   ├── service-common/       Non-deployable shared technical library
│   ├── gateway-service/      Public routing, demo login, correlation, and rate limiting
│   ├── catalog-service/      Products, categories, outbox, and search
│   ├── inventory-service/    Durable stock and Redis TCC reservations
│   └── order-service/        Order workflow, sharding, Kafka, and Feign
├── frontend/                 Next.js storefront
│   └── src/
│       ├── app/              Route entry points
│       ├── features/         Catalog, cart, inventory, orders, and seller code
│       ├── shared/           Reusable UI, hooks, API support, and common types
│       └── test/             Cross-feature component and utility tests
├── deploy/
│   ├── local/                Compose, Dockerfiles, Nginx, and bootstrap SQL
│   └── aws/                  Runnable TypeScript AWS CDK/CloudFormation project
├── scripts/                  Local lifecycle, smoke, and demo-data commands
├── docs/
│   ├── architecture/         Design and delivery documentation
│   ├── api/                  Swagger UI guide and OpenAPI contract
│   └── getting-started.md    Complete development guide
└── pom.xml                   Maven parent and module aggregator
```

## Backend modules

The root Maven reactor owns dependency versions, Java 21 compilation, formatting, unit tests,
integration tests, and coverage configuration. The four `*-service` modules are independently
deployable Spring Boot applications. `service-common` is a JAR with no application bootstrap.

Services do not import one another's implementation classes or share database schemas. Order calls
Inventory and Catalog through client interfaces implemented with OpenFeign. Kafka event payloads
remain integration contracts owned by the service that publishes them.

### Business-service package convention

Catalog, Inventory, and Order use a conventional layered structure with interfaces between the
controller, service, persistence, and external-system implementations:

```text
src/main/java/com/vincentwang/ecommerce/<service>/
├── <Service>ServiceApplication.java
├── controller/
│   ├── <Service>Controller.java
│   └── dto/
│       ├── request/
│       └── response/
├── service/
│   ├── I<Service>Service.java
│   ├── <Service>Service.java
│   └── exception/
├── model/                     Business entities, statuses, and internal data shapes
├── repository/
│   ├── I<Entity>Repository.java
│   └── mybatis/               MyBatis implementations and mapper interfaces
├── client/                    OpenFeign interfaces and adapters when needed
├── messaging/                 Kafka consumers, publishers, events, and outbox records
├── redis/                     Redis contracts, implementation, and Lua wiring when needed
├── search/                    Elasticsearch adapter and configuration when needed
├── scheduler/                 Background expiry and synchronization jobs
├── id/                        ID contract and implementation when needed
└── configuration/             Remaining framework configuration
```

The normal call direction is:

```text
Controller → IService → Service → model
                              ↓
                 repository/client interface
                              ↓
                 explicitly named implementation
```

Request and response DTOs stay beside the controllers. The service interfaces use those DTOs directly
when another command or query type would only duplicate the same fields. The shared `model` package
contains business state and internal shapes such as `ProductQuote`, `ProductSearchDocument`, and
`InventorySyncTask`.

### Package responsibilities

- `controller` validates HTTP input and maps routes. Controllers depend on `IService`, never directly
  on repositories or technology implementations.
- `service` owns orchestration, transactions, the public use-case interface, its implementation, and
  business exceptions.
- `model` owns business entities, statuses, and internal data passed between service dependencies.
- `repository` owns persistence interfaces; `repository/mybatis` contains their MyBatis implementations.
- `client`, `redis`, `search`, `messaging`, and `id` keep their interfaces beside explicitly named
  implementations so the dependency is easy to find.
- `scheduler` owns background jobs, while `configuration` contains general framework wiring.
- Interfaces use the requested `I` prefix; abstract classes use `A`; concrete technology adapters use
  explicit names such as `MyBatisOrderRepository`, `KafkaOrderOutboxPublisher`, and
  `SnowflakeOrderIdGenerator`.

ArchUnit package tests enforce three simple rules: controllers cannot skip the service layer, services
cannot use concrete technology implementations, and models remain framework-independent. Test
packages mirror production packages. Docker-backed tests end in `IntegrationTest`.

### Gateway exception

Gateway owns no product, stock, or order data, so it uses only packages that describe its technical
responsibilities:

```text
gateway/
├── GatewayServiceApplication.java
├── controller/               Username-only local login
├── routing/                  Public service routes
├── filter/
└── ratelimit/
```

### Shared-library boundary

`service-common` contains only stable technical behavior used identically by multiple services:

```text
common/
├── configuration/
├── error/
├── messaging/
├── pagination/
└── web/
```

Service-specific entities, exceptions, repositories, workflows, outbox records, and topic decisions
remain inside their owning service. A type is not moved to common merely because two files look
similar.

## Frontend ownership

The storefront is organized by business feature:

```text
src/
├── app/                       Next.js routes and composition
├── features/
│   ├── catalog/               Browse, search, category, and product UI/data
│   ├── cart/                  Cart state, validation, drawer, and line items
│   ├── inventory/             Stock loading and presentation
│   ├── orders/                Order helpers, status, timeline, and countdown
│   └── seller/                Product editing and seller validation
├── shared/
│   ├── components/            Generic layout and interaction components
│   ├── hooks/                 Generic React behavior and account context
│   └── lib/                   Base API client, formatting, store, toast, and API types
└── test/
```

## AWS deployment ownership

`deploy/aws` is a separate TypeScript project because infrastructure dependencies and release
commands do not belong in the Maven reactor or Next.js application:

```text
deploy/aws/
├── bin/ecommerce-platform.ts   Stack composition and account/region selection
├── lib/config.ts               Fixed resource sizes
├── lib/network-stack.ts        VPC, subnets, and security groups
├── lib/data-stack.ts           RDS, Redis, MSK, OpenSearch, and secrets
├── lib/compute-stack.ts        Backend image, EC2 Auto Scaling, ALB, and user data
├── lib/edge-stack.ts           S3, CloudFront, VPC origin, and WAF
├── scripts/                    One-command synthesis and deployment
└── test/                       Configuration and stack-definition checks
```

CDK is the authoring layer; CloudFormation remains the deployment engine and source of resource
state. The selected AWS profile, account ID, and region live in the Git-ignored
`deploy/aws/aws-account.env`; secret credentials remain in the AWS CLI credential store.
