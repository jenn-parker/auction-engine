package com.jennparker.auction.domain;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Immutable auction snapshot; each lot is associated with exactly one auction. */
public final class Auction {
    private final String id;
    private final Instant scheduledAt;
    private final PhysicalLocation physicalLocation;
    private final List<Lot> lots;
    private final AuctionState state;
    private final Instant publishedAt;
    private final Instant startedAt;
    private final Instant closedAt;

    public Auction(String id, Instant scheduledAt, PhysicalLocation physicalLocation, List<Lot> lots) {
        this(id, scheduledAt, physicalLocation, lots, AuctionState.NOT_YET_PUBLIC, null, null, null);
    }

    public Auction(String id, Instant scheduledAt, PhysicalLocation physicalLocation, List<Lot> lots,
                   AuctionState state, Instant publishedAt, Instant startedAt, Instant closedAt) {
        if (id == null || scheduledAt == null) throw new IllegalArgumentException("Auction id and scheduled time are required");
        this.id = id; this.scheduledAt = scheduledAt; this.physicalLocation = physicalLocation;
        this.state = state; this.publishedAt = publishedAt; this.startedAt = startedAt; this.closedAt = closedAt;
        this.lots = Collections.unmodifiableList(new ArrayList<Lot>(lots == null ? Collections.<Lot>emptyList() : lots));
        for (Lot lot : this.lots) {
            if (!id.equals(lot.getAuctionId())) throw new IllegalArgumentException("Lot belongs to another auction");
        }
    }
    public String getId() { return id; }
    public Instant getScheduledAt() { return scheduledAt; }
    public PhysicalLocation getPhysicalLocation() { return physicalLocation; }
    public List<Lot> getLots() { return lots; }
    public AuctionState getState() { return state; }
    public Instant getPublishedAt() { return publishedAt; }
    public Instant getStartedAt() { return startedAt; }
    public Instant getClosedAt() { return closedAt; }

    public Auction transitionTo(AuctionState next, Instant at) {
        boolean valid = (state == AuctionState.NOT_YET_PUBLIC && next == AuctionState.UPCOMING)
                || (state == AuctionState.UPCOMING && next == AuctionState.LIVE)
                || (state == AuctionState.LIVE && next == AuctionState.CLOSED);
        if (!valid) throw new IllegalStateException("Invalid auction transition " + state + " -> " + next);
        Instant published = next == AuctionState.UPCOMING ? at : publishedAt;
        Instant started = next == AuctionState.LIVE ? at : startedAt;
        Instant closed = next == AuctionState.CLOSED ? at : closedAt;
        return new Auction(id, scheduledAt, physicalLocation, lots, next, published, started, closed);
    }
}
