package com.jennparker.auction.persistence;

import com.jennparker.auction.domain.Bid;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/** Immutable bid-history row. The current winner is maintained on LotEntity for atomic CAS updates. */
@Entity
@Table(name = "bids")
public class BidEntity {
    @Id
    private UUID id;
    @Column(name = "lot_id", nullable = false)
    private String lotId;
    @Column(name = "customer_id", nullable = false)
    private String customerId;
    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal amount;
    @Column(name = "placed_at", nullable = false)
    private Instant placedAt;
    @Column(name = "accepted_at", nullable = false, insertable = false, updatable = false)
    private Instant acceptedAt;

    protected BidEntity() { }

    public BidEntity(String lotId, Bid bid) {
        this.id = UUID.randomUUID();
        this.lotId = lotId;
        this.customerId = bid.getCustomerId();
        this.amount = bid.getAmount();
        this.placedAt = bid.getTimestamp();
    }

    public UUID getId() { return id; }
    public String getLotId() { return lotId; }
    public String getCustomerId() { return customerId; }
    public BigDecimal getAmount() { return amount; }
    public Instant getPlacedAt() { return placedAt; }
    public Instant getAcceptedAt() { return acceptedAt; }
}
