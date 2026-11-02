package com.jennparker.auction.domain;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public final class Order {
    private final String id;
    private final String lotId, customerId;
    private final BigDecimal amount;
    private final Instant createdAt;
    private volatile Status status;
    public Order(String lotId, String customerId, BigDecimal amount, Instant createdAt) {
        this(UUID.randomUUID().toString(), lotId, customerId, amount, createdAt, Status.PENDING);
    }
    public Order(String id, String lotId, String customerId, BigDecimal amount, Instant createdAt, Status status) {
        this.id = id; this.lotId = lotId; this.customerId = customerId; this.amount = amount; this.createdAt = createdAt; this.status = status;
    }
    public String getId() { return id; }
    public String getLotId() { return lotId; }
    public String getCustomerId() { return customerId; }
    public BigDecimal getAmount() { return amount; }
    public Instant getCreatedAt() { return createdAt; }
    public Status getStatus() { return status; }
    public synchronized void transitionTo(Status next) {
        if (!((status == Status.PENDING && next == Status.PROCESSING) || (status == Status.PROCESSING && (next == Status.SETTLED || next == Status.FAILED))))
            throw new IllegalStateException("Invalid order transition " + status + " -> " + next);
        status = next;
    }
    public enum Status { PENDING, PROCESSING, SETTLED, FAILED }
}
