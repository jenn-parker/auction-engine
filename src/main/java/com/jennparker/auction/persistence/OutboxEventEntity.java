package com.jennparker.auction.persistence;

import com.jennparker.auction.domain.Order;

import javax.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "outbox_events")
public class OutboxEventEntity {
    @Id private UUID id;
    @Column(name = "event_type", nullable = false) private String eventType;
    @Column(name = "order_id", nullable = false) private UUID orderId;
    @Column(nullable = false) private String status;
    @Column(nullable = false) private int attempts;
    @Column(name = "available_at", nullable = false) private Instant availableAt;
    @Column(name = "locked_until") private Instant lockedUntil;
    @Column(name = "last_error") private String lastError;
    @Column(name = "created_at", nullable = false) private Instant createdAt;
    @Column(name = "published_at") private Instant publishedAt;

    protected OutboxEventEntity() { }

    public OutboxEventEntity(Order order) {
        this.id = UUID.randomUUID(); this.eventType = "PaymentAuthorizationRequested";
        this.orderId = UUID.fromString(order.getId()); this.status = "PENDING"; this.attempts = 0;
        this.availableAt = order.getCreatedAt(); this.createdAt = order.getCreatedAt();
    }

    public UUID getId() { return id; }
    public UUID getOrderId() { return orderId; }
    public String getStatus() { return status; }
    public int getAttempts() { return attempts; }
    public void claim(Instant lockedUntil) { this.status = "PROCESSING"; this.lockedUntil = lockedUntil; this.attempts++; }
    public void markPublished(Instant at) { this.status = "PUBLISHED"; this.publishedAt = at; this.lockedUntil = null; }
    public void markFailed(String error, Instant retryAt) { this.status = "PENDING"; this.lastError = error; this.availableAt = retryAt; this.lockedUntil = null; }
}
