package com.jennparker.auction.application;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;

import java.util.concurrent.TimeUnit;

/**
 * Low-cardinality business metrics for the auction hot path.
 *
 * Lot IDs, customer IDs, and idempotency keys are deliberately not metric tags;
 * they would create an unbounded time-series cardinality problem.
 */
public final class AuctionMetrics {
    private final MeterRegistry registry;
    private final boolean enabled;

    public AuctionMetrics(MeterRegistry registry) {
        this.registry = registry;
        this.enabled = registry != null;
    }

    public static AuctionMetrics noop() {
        return new AuctionMetrics(null);
    }

    public void recordBid(String outcome, String reason, long elapsedNanos) {
        if (!enabled) return;
        Counter.builder("auction.bids")
                .description("Bids received by the auction engine")
                .tag("outcome", outcome)
                .tag("reason", reason)
                .register(registry)
                .increment();
        Timer.builder("auction.bid.processing")
                .description("Time spent validating and processing a bid")
                .publishPercentiles(0.5, 0.95, 0.99)
                .register(registry)
                .record(elapsedNanos, TimeUnit.NANOSECONDS);
    }

    public void recordSettlement(String outcome, int ordersCreated, long elapsedNanos) {
        if (!enabled) return;
        Counter.builder("auction.settlements")
                .description("Lot and auction settlement attempts")
                .tag("outcome", outcome)
                .register(registry)
                .increment();
        registry.counter("auction.orders.created").increment(ordersCreated);
        Timer.builder("auction.settlement.processing")
                .description("Time spent closing lots and creating orders")
                .publishPercentiles(0.5, 0.95, 0.99)
                .register(registry)
                .record(elapsedNanos, TimeUnit.NANOSECONDS);
    }

    public void recordPayment(String outcome, boolean duplicate, long elapsedNanos) {
        if (!enabled) return;
        Counter.builder("auction.payments")
                .description("Payment authorization attempts")
                .tag("outcome", outcome)
                .tag("duplicate", Boolean.toString(duplicate))
                .register(registry)
                .increment();
        Timer.builder("auction.payment.authorization")
                .description("Time spent authorizing a payment")
                .publishPercentiles(0.5, 0.95, 0.99)
                .register(registry)
                .record(elapsedNanos, TimeUnit.NANOSECONDS);
    }
}
