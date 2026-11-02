package com.jennparker.auction.domain;

import java.math.BigDecimal;
import java.time.Instant;

public final class Bid {
    private final String customerId;
    private final BigDecimal amount;
    private final Instant timestamp;
    private final Instant acceptedAt;

    public Bid(String customerId, BigDecimal amount, Instant timestamp) {
        this(customerId, amount, timestamp, null);
    }
    public Bid(String customerId, BigDecimal amount, Instant timestamp, Instant acceptedAt) {
        this.customerId = customerId; this.amount = amount; this.timestamp = timestamp; this.acceptedAt = acceptedAt;
    }
    public String getCustomerId() { return customerId; }
    public BigDecimal getAmount() { return amount; }
    public Instant getTimestamp() { return timestamp; }
    public Instant getAcceptedAt() { return acceptedAt; }
}
