package com.jennparker.auction.persistence;

import javax.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "payment_allocations")
public class PaymentAllocationEntity {
    @Id private UUID id;
    @Column(name = "order_id", nullable = false) private UUID orderId;
    @Column(name = "payment_id") private UUID paymentId;
    @Column(name = "payment_method_id") private String paymentMethodId;
    @Column(nullable = false, precision = 19, scale = 4) private BigDecimal amount;
    @Column(nullable = false) private String status;
    @Column(name = "created_at", nullable = false) private Instant createdAt;
    protected PaymentAllocationEntity() { }
    public PaymentAllocationEntity(UUID orderId, String paymentMethodId, BigDecimal amount, String status) {
        this(null, orderId, paymentMethodId, amount, status);
    }
    public PaymentAllocationEntity(UUID paymentId, UUID orderId, String paymentMethodId, BigDecimal amount, String status) {
        this.id = UUID.randomUUID(); this.paymentId = paymentId; this.orderId = orderId; this.paymentMethodId = paymentMethodId; this.amount = amount; this.status = status; this.createdAt = Instant.now();
    }
}
