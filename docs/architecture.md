# Architecture overview

```mermaid
flowchart LR
  Client[Web or mobile client] --> API[Spring Boot API]
  API --> Auth[Cognito JWT validation]
  API --> Bid[Bid ingestion]
  Bid --> DB[(PostgreSQL)]
  DB --> Outbox[Transactional outbox]
  Outbox --> SQS[(SQS / LocalStack)]
  SQS --> Worker[Payment worker]
  Worker --> Gateway[Payment gateway adapter]
  Worker --> DB
  API --> Metrics[Actuator + Prometheus]
```

## Integrity boundaries

- PostgreSQL is authoritative for persisted bid ordering, bid history, orders,
  payment attempts, and outbox delivery state.
- Bid ingestion uses a compare-and-set update so concurrent requests cannot
  overwrite a newer bid. The accepted bid and its history row commit together.
- Settlement creates the order and its payment event in one transaction. The
  outbox makes event publication recoverable after a process restart.
- SQS is deliberately treated as at-least-once delivery. The payment worker
  uses durable idempotency claims before calling the gateway.
- Payment methods store provider-issued instrument references only. Raw card
  data remains outside this service.

See the ADRs in [`adr/`](adr/) for the key design decisions.
