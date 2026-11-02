package com.jennparker.auction.persistence;

import com.jennparker.auction.domain.Auction;
import com.jennparker.auction.domain.PhysicalLocation;
import com.jennparker.auction.domain.AuctionState;
import javax.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "auctions")
public class AuctionEntity {
    @Id private String id;
    @Column(name = "scheduled_at", nullable = false) private Instant scheduledAt;
    @Column(nullable = false) private String state;
    @Column(name = "published_at") private Instant publishedAt;
    @Column(name = "started_at") private Instant startedAt;
    @Column(name = "closed_at") private Instant closedAt;
    @Column(name = "venue_name") private String venueName;
    @Column(name = "location_line1") private String line1;
    @Column(name = "location_line2") private String line2;
    @Column(name = "location_city") private String city;
    @Column(name = "location_state_or_region") private String stateOrRegion;
    @Column(name = "location_postal_code") private String postalCode;
    @Column(name = "location_country_code") private String countryCode;

    protected AuctionEntity() { }
    public AuctionEntity(Auction auction) {
        this.id = auction.getId(); this.scheduledAt = auction.getScheduledAt(); this.state = auction.getState().name();
        this.publishedAt = auction.getPublishedAt(); this.startedAt = auction.getStartedAt(); this.closedAt = auction.getClosedAt();
        PhysicalLocation location = auction.getPhysicalLocation();
        if (location != null) {
            this.venueName = location.getVenueName(); this.line1 = location.getLine1(); this.line2 = location.getLine2();
            this.city = location.getCity(); this.stateOrRegion = location.getStateOrRegion();
            this.postalCode = location.getPostalCode(); this.countryCode = location.getCountryCode();
        }
    }
    public Auction toDomain(java.util.List<com.jennparker.auction.domain.Lot> lots) {
        PhysicalLocation location = venueName == null && line1 == null && city == null ? null
                : new PhysicalLocation(venueName, line1, line2, city, stateOrRegion, postalCode, countryCode);
        return new Auction(id, scheduledAt, location, lots, AuctionState.valueOf(state), publishedAt, startedAt, closedAt);
    }
    public void updateFrom(Auction auction) {
        this.scheduledAt = auction.getScheduledAt();
        this.state = auction.getState().name(); this.publishedAt = auction.getPublishedAt();
        this.startedAt = auction.getStartedAt(); this.closedAt = auction.getClosedAt();
        PhysicalLocation location = auction.getPhysicalLocation();
        this.venueName = location == null ? null : location.getVenueName();
        this.line1 = location == null ? null : location.getLine1();
        this.line2 = location == null ? null : location.getLine2();
        this.city = location == null ? null : location.getCity();
        this.stateOrRegion = location == null ? null : location.getStateOrRegion();
        this.postalCode = location == null ? null : location.getPostalCode();
        this.countryCode = location == null ? null : location.getCountryCode();
    }
    public String getId() { return id; }
}
