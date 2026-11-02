# ADR 0002: Transactional outbox and idempotent payment worker

## Status

Accepted

## Decision

Settlement writes the order and a pending outbox event in one transaction. A
publisher sends pending events to SQS. The payment worker claims durable
payment idempotency records before authorizing a gateway request and treats SQS
delivery as at-least-once.

## Why

Directly publishing after a database commit creates a crash window. Publishing
before the commit creates a different inconsistency. The outbox closes that
gap, while the durable idempotency claim prevents retries or redelivery from
double-authorizing a payment.

## Trade-offs

Payment processing becomes asynchronous and requires retry/backoff and
dead-letter operations. The API and worker must expose enough metrics to make
lag and failed authorization visible.
