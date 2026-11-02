package com.jennparker.auction.domain;

import java.time.Clock;
import java.time.Instant;
import java.util.concurrent.atomic.AtomicReference;
import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;

/** A lot owns its CAS-protected highest bid and settlement transition. */
public final class Lot {
    private final String id;
    private final String auctionId;
    private final Instant closesAt;
    private final Instant opensAt;
    private final String category, currency;
    private final LotCondition condition;
    private final String conditionNotes;
    private final Provenance provenance;
    private final LotMeasurements measurements;
    private final List<FulfillmentOption> fulfillmentOptions;
    private final List<String> imageUrls;
    private final BigDecimal reservePrice, buyerPremiumRate, taxRate;
    private volatile String winningCustomerId;
    private volatile BigDecimal winningBidAmount;
    private volatile Instant settledAt;
    private final AtomicReference<Bid> highestBid = new AtomicReference<Bid>();
    private final AtomicReference<Status> status = new AtomicReference<Status>(Status.OPEN);

    public Lot(String id, Instant closesAt) { this(id, null, closesAt); }
    public Lot(String id, String auctionId, Instant closesAt) {
        this(id, auctionId, null, closesAt, null, LotCondition.UNKNOWN, null, null, null, Collections.<FulfillmentOption>emptyList(),
                Collections.<String>emptyList(), null, "USD", BigDecimal.ZERO, BigDecimal.ZERO);
    }
    public Lot(String id, String auctionId, Instant opensAt, Instant closesAt, String category, LotCondition condition,
               String conditionNotes, Provenance provenance, LotMeasurements measurements, List<FulfillmentOption> fulfillmentOptions,
               List<String> imageUrls, BigDecimal reservePrice, String currency, BigDecimal buyerPremiumRate, BigDecimal taxRate) {
        this.id = id; this.auctionId = auctionId; this.opensAt = opensAt; this.closesAt = closesAt; this.category = category;
        this.condition = condition == null ? LotCondition.UNKNOWN : condition; this.conditionNotes = conditionNotes; this.provenance = provenance;
        this.measurements = measurements; this.fulfillmentOptions = Collections.unmodifiableList(fulfillmentOptions == null ? Collections.<FulfillmentOption>emptyList() : fulfillmentOptions);
        this.imageUrls = Collections.unmodifiableList(imageUrls == null ? Collections.<String>emptyList() : imageUrls);
        this.reservePrice = reservePrice; this.currency = currency; this.buyerPremiumRate = buyerPremiumRate; this.taxRate = taxRate;
    }
    public String getId() { return id; }
    public String getAuctionId() { return auctionId; }
    public Instant getClosesAt() { return closesAt; }
    public Instant getOpensAt() { return opensAt; } public String getCategory() { return category; }
    public LotCondition getCondition() { return condition; } public String getConditionNotes() { return conditionNotes; }
    public Provenance getProvenance() { return provenance; } public LotMeasurements getMeasurements() { return measurements; }
    public List<FulfillmentOption> getFulfillmentOptions() { return fulfillmentOptions; } public List<String> getImageUrls() { return imageUrls; }
    public BigDecimal getReservePrice() { return reservePrice; } public String getCurrency() { return currency; }
    public BigDecimal getBuyerPremiumRate() { return buyerPremiumRate; } public BigDecimal getTaxRate() { return taxRate; }
    public String getWinningCustomerId() { return winningCustomerId; } public BigDecimal getWinningBidAmount() { return winningBidAmount; }
    public Instant getSettledAt() { return settledAt; }
    public Bid getHighestBid() { return highestBid.get(); }
    public Status getStatus() { return status.get(); }

    /** Restores the database-backed winner into the in-memory read model. */
    public synchronized void restoreHighestBid(Bid bid) { highestBid.set(bid); }

    public synchronized boolean accept(Bid candidate, Clock clock) {
        if (status.get() != Status.OPEN || !candidate.getTimestamp().isBefore(closesAt)
                || !clock.instant().isBefore(closesAt)) return false;
        while (true) {
            Bid current = highestBid.get();
            if (current != null && candidate.getAmount().compareTo(current.getAmount()) <= 0) return false;
            if (highestBid.compareAndSet(current, new Bid(candidate.getCustomerId(), candidate.getAmount(), candidate.getTimestamp(), clock.instant()))) return true;
        }
    }

    public synchronized Bid close(Clock clock) {
        if (clock.instant().isBefore(closesAt) || !status.compareAndSet(Status.OPEN, Status.CLOSED)) return null;
        return captureWinner(highestBid.get(), clock.instant());
    }

    /** Closes a lot as part of an already-closed auction, regardless of its individual timer. */
    public synchronized Bid closeForAuctionSettlement() {
        if (!status.compareAndSet(Status.OPEN, Status.CLOSED)) return null;
        return captureWinner(highestBid.get(), Instant.now());
    }
    private Bid captureWinner(Bid winner, Instant settledAt) { if (winner != null) { winningCustomerId = winner.getCustomerId(); winningBidAmount = winner.getAmount(); this.settledAt = settledAt; } return winner; }
    public enum Status { OPEN, CLOSED }
}
