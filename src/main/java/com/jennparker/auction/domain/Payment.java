package com.jennparker.auction.domain;

import java.math.BigDecimal;
import java.time.Instant;

/** A single authorized payment transaction, distinct from a reusable payment method. */
public final class Payment {
    private final String id;
    private final String customerId;
    private final String paymentMethodId;
    private final BigDecimal amount;
    private final String status;
    private final Instant createdAt;

    public Payment(String id, String customerId, String paymentMethodId, BigDecimal amount, String status, Instant createdAt) {
        this.id = id; this.customerId = customerId; this.paymentMethodId = paymentMethodId;
        this.amount = amount; this.status = status; this.createdAt = createdAt;
    }
    public String getId() { return id; }
    public String getCustomerId() { return customerId; }
    public String getPaymentMethodId() { return paymentMethodId; }
    public BigDecimal getAmount() { return amount; }
    public String getStatus() { return status; }
    public Instant getCreatedAt() { return createdAt; }
}
