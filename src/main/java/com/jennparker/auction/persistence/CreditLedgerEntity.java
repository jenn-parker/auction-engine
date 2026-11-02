package com.jennparker.auction.persistence;

import javax.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "customer_credit_ledger")
public class CreditLedgerEntity {
    @Id private UUID id;
    @Column(name = "customer_id", nullable = false) private String customerId;
    @Column(nullable = false, precision = 19, scale = 4) private BigDecimal amount;
    @Column(nullable = false) private String reason;
    @Column(name = "reference_id") private String referenceId;
    @Column(name = "created_at", nullable = false) private Instant createdAt;
    protected CreditLedgerEntity() { }
    public CreditLedgerEntity(String customerId, BigDecimal amount, String reason, String referenceId) {
        this.id = UUID.randomUUID(); this.customerId = customerId; this.amount = amount; this.reason = reason; this.referenceId = referenceId; this.createdAt = Instant.now();
    }
    public BigDecimal getAmount() { return amount; }
}
