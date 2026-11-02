# Portfolio demo flow

This is the shortest story to demonstrate the design in an interview or code
review. Start the stack with `docker compose up --build`, then use the examples
below with a local API at `http://localhost:8080`.

1. Create an auction with `POST /api/v1/auctions`.
2. Publish and start it using `/publish` and `/start`.
3. Add a lot with `POST /api/v1/auctions/{auctionId}/lots`.
4. Submit two bids concurrently to `/api/v1/lots/{lotId}/bids`. The larger
   accepted bid wins; a bid whose event timestamp is at or after the exact
   close time is rejected.
5. Close the auction with `/api/v1/auctions/{auctionId}/close`.
6. Create an authorized payment with `POST /api/v1/payments` and an
   `Idempotency-Key` header.
7. Settle the auction using the resulting `paymentId`. Settlement requires the
   authorized payment and any credits to cover the full amount owed.
8. Read `/api/v1/customers/{customerId}/invoice` to show the itemized result.

For request bodies, response examples, authentication, and all available
routes, open the [static Swagger UI](index.html).
