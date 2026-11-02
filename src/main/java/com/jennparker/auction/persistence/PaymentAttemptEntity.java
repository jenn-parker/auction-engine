package com.jennparker.auction.persistence;

import javax.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "payment_attempts")
public class PaymentAttemptEntity {
    @Id private UUID id;
    @Column(name = "order_id", nullable = false) private UUID orderId;
    @Column(name = "idempotency_key", nullable = false, unique = true) private String idempotencyKey;
    @Column(nullable = false) private String status;
    @Column(name = "gateway_reference") private String gatewayReference;
    @Column(name = "created_at", nullable = false) private Instant createdAt;

    protected PaymentAttemptEntity() { }

    public String getIdempotencyKey() { return idempotencyKey; }
    public String getStatus() { return status; }
}
