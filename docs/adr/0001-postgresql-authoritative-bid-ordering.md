# ADR 0001: PostgreSQL is authoritative for bid ordering

## Status

Accepted

## Decision

Persist the current highest bid with a PostgreSQL compare-and-set update keyed
by lot ID. The update succeeds only when the candidate is newer in the auction
ordering and the lot is still open. The accepted bid history row is inserted
in the same transaction.

## Why

An in-memory CAS is useful for isolated domain tests, but it cannot coordinate
multiple application instances. PostgreSQL provides the durable shared state
and transactional boundary required when auction traffic is horizontally
scaled.

## Trade-offs

The database is on the hot path and requires appropriate indexing and
connection-pool sizing. The design favors financial correctness over accepting
a bid that cannot be durably recorded.
