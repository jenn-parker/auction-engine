package com.jennparker.auction;

import com.jennparker.auction.application.AuctionMetrics;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AuctionMetricsTest {
    @Test
    void recordsLowCardinalityBusinessMetrics() {
        SimpleMeterRegistry registry = new SimpleMeterRegistry();
        AuctionMetrics metrics = new AuctionMetrics(registry);

        metrics.recordBid("accepted", "none", 1_000_000);
        metrics.recordBid("rejected", "invalid_bid", 2_000_000);
        metrics.recordSettlement("success", 1, 3_000_000);
        metrics.recordPayment("settled", false, 4_000_000);
        metrics.recordPayment("duplicate", true, 5_000_000);

        assertEquals(1.0, registry.get("auction.bids").tag("outcome", "accepted").counter().count());
        assertEquals(1.0, registry.get("auction.bids").tag("reason", "invalid_bid").counter().count());
        assertEquals(1.0, registry.get("auction.orders.created").counter().count());
        assertEquals(1.0, registry.get("auction.payments").tag("duplicate", "true").counter().count());
        assertEquals(2, registry.get("auction.bid.processing").timer().count());
    }
}
