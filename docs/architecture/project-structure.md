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
Inventory and Catalog through application client interfaces implemented with OpenFeign. Kafka event
payloads are integration contracts rather than shared service domain classes.

### Business-service package convention

Catalog, Inventory, and Order use the same pragmatic DDD and Clean Architecture layout:

```text
src/main/java/com/vincentwang/ecommerce/<service>/
├── <Service>ServiceApplication.java
├── api/
│   ├── <Service>Controller.java
│   └── dto/
│       ├── request/
│       └── response/
├── application/
│   ├── I<Service>Service.java
│   ├── <Service>ApplicationService.java
│   ├── I<Dependency>.java        Explicit client or capability contracts
│   ├── exception/
│   ├── model/                 Internal models that differ from API DTOs
│   └── repository/            Persistence contracts
├── domain/
│   └── model/                 Business entities, value objects, statuses, and invariants
└── infrastructure/
    ├── configuration/
    ├── persistence/
    └── <technology>/          Messaging, Redis, search, clients, schedulers, or ID generation
```

The normal call direction is:

```text
Controller → IService → ApplicationService → domain model
                                  ↓
                     application repository/client interface
                                  ↑ implemented by
                     infrastructure adapter
```

This project intentionally uses API request/response DTOs directly at the application boundary when
an additional command or query type would contain the same fields. A separate application model is
created only when its meaning or shape differs, such as `ProductQuote`, `ProductSearchDocument`, or
`InventorySyncTask`.

### Package responsibilities

- `api` validates HTTP input and maps HTTP routes. Controllers depend on `IService`, never concrete
  services or infrastructure.
- `application` owns orchestration, transactions, use-case interfaces, application failures, and
  contracts required from external technology.
- `domain` owns business state and rules. Redis-, Kafka-, MyBatis-, Feign-, and HTTP-specific names do
  not belong here.
- `infrastructure` implements application contracts and contains framework wiring.
- Interfaces use the requested `I` prefix; abstract classes use `A`; concrete technology adapters use
  explicit names such as `MyBatisOrderRepository`, `KafkaOrderOutboxPublisher`, and
  `SnowflakeOrderIdGenerator`.

ArchUnit tests enforce that domain code remains independent and application code does not depend on
infrastructure. Test packages mirror production packages. Docker-backed tests end in
`IntegrationTest`.

### Gateway exception

Gateway is entirely technical and does not pretend to have a domain layer:

```text
gateway/
├── GatewayServiceApplication.java
├── api/                      Username-only local login
├── configuration/
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
