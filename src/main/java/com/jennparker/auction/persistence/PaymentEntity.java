package com.jennparker.auction.persistence;

import com.jennparker.auction.domain.Payment;
import com.jennparker.auction.domain.PaymentStatus;
import javax.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "payments")
public class PaymentEntity {
    @Id private UUID id;
    @Column(name = "customer_id", nullable = false) private String customerId;
    @Column(name = "payment_method_id", nullable = false) private String paymentMethodId;
    @Column(nullable = false, precision = 19, scale = 4) private BigDecimal amount;
    @Column(nullable = false) private String status;
    @Column(name = "idempotency_key", unique = true) private String idempotencyKey;
    @Column(name = "created_at", nullable = false) private Instant createdAt;
    protected PaymentEntity() { }
    public PaymentEntity(String customerId, String paymentMethodId, BigDecimal amount, String status) {
        this(customerId, paymentMethodId, amount, status, null);
    }
    public PaymentEntity(String customerId, String paymentMethodId, BigDecimal amount, String status, String idempotencyKey) {
        this.id = UUID.randomUUID(); this.customerId = customerId; this.paymentMethodId = paymentMethodId;
        this.amount = amount; this.status = status; this.idempotencyKey = idempotencyKey; this.createdAt = Instant.now();
    }
    public UUID getId() { return id; }
    public String getCustomerId() { return customerId; }
    public String getPaymentMethodId() { return paymentMethodId; }
    public BigDecimal getAmount() { return amount; }
    public String getStatus() { return status; }
    public boolean isAuthorized() { return PaymentStatus.AUTHORIZED.name().equals(status); }
    public void updateStatus(String status) { this.status = status; }
    public Payment toDomain() { return new Payment(id.toString(), customerId, paymentMethodId, amount, status, createdAt); }
}
