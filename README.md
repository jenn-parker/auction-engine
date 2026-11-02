# BACKEND-2026 — Real-Time Auction Bidding & Payment Orchestration Engine

Java 8 / Spring Boot reference implementation for thread-safe bid ingestion, deterministic settlement, and idempotent payment authorization.

## Run

Requires JDK 8 and Maven 3.6+:

```bash
mvn test
mvn spring-boot:run
```

To run the application and PostgreSQL together with Docker:

```bash
cp .env.example .env
# Edit .env and set LOCALSTACK_AUTH_TOKEN
docker compose up --build
```

PostgreSQL is exposed on `localhost:5432`, LocalStack on `localhost:4566`, and the API on `localhost:8080`. LocalStack bootstraps an SQS `auction-payment-events` queue and a mock payment-gateway secret. Flyway applies versioned migrations from `src/main/resources/db/migration` on startup. The application uses `ddl-auto=validate`, so schema changes must be made through migrations rather than Hibernate auto-generation.

Observability is available through Spring Boot Actuator: `/actuator/health`, `/actuator/metrics`, and `/actuator/prometheus`. Business metrics include bid acceptance/rejection counts and latency, settlement outcomes, orders created, and payment authorization outcomes/latency. Metric tags intentionally use bounded values such as outcome and reason; IDs are never emitted as tags.

Cognito JWT protection is enabled with `COGNITO_ENABLED=true` and `COGNITO_ISSUER_URI` set to the Cognito User Pool issuer URL. When enabled, Spring Security validates bearer-token signatures, issuer, expiry, and scopes. The expected scopes are `auction:bid`, `auction:manage`, `auction:settle`, and `auction:admin`. Management mutations additionally require the JWT `sub` to map to an active `auctioneers` row. Provision auctioneers through `POST /api/v1/internal/auctioneers` using an admin-scoped token. Local Docker defaults to disabled so the stack can be explored without a user pool; this must be enabled in deployed environments.

The LocalStack token is a developer credential and must remain in `.env`, which is ignored by Git. For CI, use a dedicated CI token stored in the CI provider's secret store rather than a personal developer token.

## Architecture

- `domain`: immutable bid/order data, a lot state machine, and eligibility ports.
- `application`: CAS-based bid acceptance and payment orchestration ports.
- `api`: thin HTTP mappings for auction management, lot reads, bidding, settlement, customer, payment-method, and payment-resource endpoints.
- `config`: replaceable adapters; the default customer service is intentionally permissive for local bootstrapping.
- `persistence`: Spring Data JPA persistence representations and repositories; these are deliberately separate from the domain objects.
- `infrastructure.aws`: AWS SDK adapters; SQS is used as the boundary for asynchronous payment authorization events.

Each lot uses `AtomicReference.compareAndSet` for the isolated in-memory path, while the configured application uses a PostgreSQL compare-and-set update as the authoritative bid race. A successful bid update and immutable `bids` history insert share one transaction. Closing uses a one-way atomic `OPEN -> CLOSED` transition, so only one settlement can create an order. Orders permit only `PENDING -> PROCESSING -> SETTLED|FAILED`.

Bid payload timestamps are retained as client-provided audit data only. Bid admission uses PostgreSQL `clock_timestamp()` and records a server-generated acceptance timestamp on both the lot and bid-history row, so the exact close-time decision is based on the database clock rather than a customer device clock.

Settlement writes the order and a `PENDING` transactional outbox event in the same database transaction. A scheduled outbox publisher claims rows with PostgreSQL row locks, publishes them to SQS, and retries failures with bounded backoff. The payment worker consumes SQS messages, authorizes payments, persists the order state, and deletes messages only after successful processing. SQS remains at-least-once delivery, so the worker is written to tolerate redelivery.

Payment retries use `lotId:winningBidderId` as the idempotency key. Production authorization claims are persisted in the existing `payment_attempts` table using an atomic unique insert, so concurrent workers and process restarts cannot claim the same payment twice. The in-memory fallback exists only for isolated unit tests.

The SQS publisher is configured through `AWS_ENDPOINT`, `AWS_REGION`, credentials, and `PAYMENT_QUEUE_URL`. Docker uses LocalStack; an AWS deployment can omit `AWS_ENDPOINT` and use IAM task-role credentials instead.

Customers are immutable snapshots with name, email, phone, postal `Address`, account status, bidding permission, and authorized `PaymentMethod` records. Payment methods contain gateway-safe metadata only: provider, tokenized instrument ID, type, authorization flag, default flag, and optional card expiration month/year. Raw card numbers and security codes must remain inside a PCI-compliant payment provider. Customer PII must not be written to bid logs or payment idempotency keys.

## API

### Static Swagger documentation

The repository includes a static, hiring-manager-friendly API reference at
[`docs/index.html`](docs/index.html), backed by the checked-in
[`docs/openapi.yaml`](docs/openapi.yaml). It does not require the application,
PostgreSQL, LocalStack, or Cognito to be running. For local preview, serve the
directory so the browser can load the YAML contract:

```bash
python3 -m http.server 8000 --directory docs
```

Then open <http://localhost:8000>. The same `docs` directory can be selected as
the GitHub Pages publishing folder to give reviewers a public [`/docs`](https://jenn-parker.github.io/auction-engine/) site.

The [architecture overview](https://jenn-parker.github.io/auction-engine/architecture/), [portfolio demo flow](https://jenn-parker.github.io/auction-engine/demo),
and [architecture decision records](https://jenn-parker.github.io/auction-engine/adr/) explain the main design choices
without requiring a code walkthrough.

`POST /api/v1/lots` is retained as a compatibility path but now requires `auctionId` in the request body. Lot IDs are unique.

`POST /api/v1/auctions` creates a scheduled auction with optional physical-location data. `POST /api/v1/auctions/{auctionId}/lots` creates a lot owned by that auction, `GET /api/v1/auctions/{auctionId}` returns the auction with its collection of lots, and `GET /api/v1/auctions/{auctionId}/lots` returns the lot collection directly. `GET /api/v1/lots/{lotId}` returns an individual lot. The auction-scoped creation endpoint is the preferred path because a lot must belong to exactly one auction.

Auction lifecycle transitions use `POST /api/v1/auctions/{auctionId}/publish`, `/start`, and `/close`. The allowed sequence is `NOT_YET_PUBLIC -> UPCOMING -> LIVE -> CLOSED`; bids are accepted only while the parent auction is `LIVE`.

Settlement supports both `POST /api/v1/internal/lots/{lotId}/settle` and `POST /api/v1/internal/auctions/{auctionId}/settle`. Auction-level settlement requires `CLOSED` state and creates orders for lots with winning bids.

## Production considerations

This is a portfolio reference implementation, not a PCI-certified payment
system. A production deployment would additionally require:

- managed PostgreSQL high availability, connection-pool limits, indexes, and
  load tests against realistic auction-spike traffic;
- an SQS dead-letter queue, alerting on outbox age and worker lag, and an
  operational replay/runbook for failed messages;
- a real payment-provider adapter with provider reconciliation, webhook
  handling, refund flows, and carefully scoped secrets/IAM permissions;
- synchronized server/database clocks and an explicit policy for late events;
- dashboards and alerts for bid rejection rates, settlement failures,
  authorization latency, queue depth, and database contention; and
- formal threat modeling, PII retention controls, audit logging, and a review
  of Cognito scopes and auctioneer provisioning permissions.

Auctions support `PUT` and `PATCH` at `/api/v1/auctions/{auctionId}`. Lots support `PUT` and `PATCH` at `/api/v1/lots/{lotId}` for closing-time changes, plus `DELETE /api/v1/lots/{lotId}`. Closed lots cannot be modified, and lots referenced by bids or orders cannot be deleted.

`POST /api/v1/customers` creates a customer and `PUT /api/v1/customers/{customerId}` replaces the editable customer profile. Customer records are persisted in PostgreSQL. Example payload:

```json
{
  "customerId": "cust_8829104",
  "name": "Alex Parker",
  "email": "alex@example.com",
  "phone": "+1-555-0100",
  "address": { "line1": "1 Main Street", "city": "Chicago", "stateOrRegion": "IL", "postalCode": "60601", "countryCode": "US" },
  "paymentMethod": { "paymentMethodId": "pm_local_1", "type": "CARD", "authorized": true }
}
```

Payment credentials are intentionally not accepted by these endpoints. `paymentMethodId` is a provider-issued tokenized instrument reference, paired with its `provider`; card methods may also include `expirationMonth` and `expirationYear`. These are safe metadata for this service to retain, but are not a PAN, CVV, bank account number, or other raw credential.

`PATCH /api/v1/customers/{customerId}` applies partial profile changes and preserves omitted fields. Payment methods are managed separately with `GET`, `POST`, `PATCH`, and `DELETE` at `/api/v1/customers/{customerId}/payment-methods`. Each method stores a provider-issued token, supports an `isDefault` flag, and can be reused for future payments; PostgreSQL enforces at most one default method per customer. Only provider-safe references and authorization state are accepted; raw card data is never handled by this service.

`GET /api/v1/customers/{customerId}/balance` returns the customer’s unpaid orders and `totalAmountDue`. `PENDING`, `PROCESSING`, and `FAILED` orders are included; `SETTLED` orders are excluded. The current total reflects persisted order amounts; buyer premium and tax calculation should be incorporated into order totals before payment collection.

`GET /api/v1/customers/{customerId}/invoice` returns every won lot represented by an order, with its cost, payment status, amount paid, and amount owed, along with invoice totals.

`POST /api/v1/payments` authorizes a saved payment instrument and creates a payment transaction. It requires an `Idempotency-Key` header and a body containing `customerId`, `paymentMethodId`, and positive `amount`. Payment status moves through `PROCESSING`, `AUTHORIZED`, or `FAILED`; settlement moves an authorized payment to `COMPLETED`. `GET /api/v1/payments/{paymentId}` returns a payment by ID. `GET /api/v1/customers/{customerId}/payments` lists the customer’s individual payment transactions, and the customer-scoped detail endpoint returns one transaction. These endpoints are grouped under the `Payments` OpenAPI tag. A payment is distinct from a payment method: payment methods are reusable tokenized instruments, while payments are immutable financial transactions that can later be applied during settlement.

`POST /api/v1/lots/{lotId}/bids` accepts `{ "customerId": "cust_1", "bidAmount": 16500.00, "timestamp": "2026-11-01T20:14:32Z" }` and returns `202`, `400`, or `403` according to validation.

`POST /api/v1/internal/lots/{lotId}/settle` accepts a payment plan such as `{ "customerId": "cust-1", "payments": [{ "paymentId": "pay-1", "amount": 125.00 }], "creditAmount": 0.00 }`. Payment IDs must belong to the customer and be `AUTHORIZED`; applying them transitions them to `COMPLETED`. Legacy method/amount entries remain supported for the synchronous authorization path. All payments must fully cover the lot, with excess becoming customer credit. `POST /api/v1/internal/auctions/{auctionId}/settle` additionally requires `customerId` and applies the authorized payment plan before settling that customer’s winning lots.

## Testing rule

Core tests must inject `CustomerService`, `PaymentGateway`, `Clock`, and any future persistence ports as mocks/fakes. No database, network, or live payment gateway is allowed on the unit/integration test path. Add adapter tests separately behind an explicit profile.

The test suite currently covers:

- concurrent bid races and payment retries;
- auction and order state-machine transitions;
- customer/payment-method eligibility and immutable snapshots;
- service-layer authorization and settlement behavior; and
- HTTP validation and response status codes through standalone MockMvc tests.

Run it with `mvn test`. The default suite is intentionally infrastructure-free; Docker/PostgreSQL and LocalStack are only required for running the application stack.

### Integration tests

The `integration` Maven profile runs `MigrationIntegrationIT` with Testcontainers and a fresh PostgreSQL 16 container. It verifies that Flyway applies every migration and that Hibernate starts successfully with `ddl-auto=validate`. Docker is required:

```bash
mvn -Pintegration verify
```

The normal `mvn test` path does not require Docker, PostgreSQL, LocalStack, or a payment gateway.

For a repeatable Java 8 throughput harness of concurrent bid acceptance, run `mvn -Pperformance test exec:java`. The benchmark is separate from the normal test naming convention and reports throughput rather than enforcing a machine-specific timing threshold.

CI runs `mvn verify`, generates a JaCoCo report, and enforces a 30% line-coverage and 25% branch-coverage baseline across the domain, application, and API layers. The baseline is intentionally ratcheted upward as persistence and management adapter tests are added.
