package com.jennparker.auction.persistence;

import javax.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.LinkedHashSet;
import com.jennparker.auction.domain.*;

/** Persistence representation; domain Lot remains framework-independent. */
@Entity
@Table(name = "lots")
public class LotEntity {
    @Id
    private String id;
    @Column(name = "closes_at", nullable = false)
    private Instant closesAt;
    @Column(name = "opens_at") private Instant opensAt;
    @Column(name = "auction_id", nullable = false)
    private String auctionId;
    @Column(nullable = false)
    private String status;
    @Column(name = "highest_bid_amount", precision = 19, scale = 4)
    private BigDecimal highestBidAmount;
    @Column(name = "highest_bidder_id")
    private String highestBidderId;
    @Column(name = "highest_bid_at")
    private Instant highestBidAt;
    @Column(name = "highest_bid_accepted_at")
    private Instant highestBidAcceptedAt;
    @Version
    private Long version;
    private String category;
    @Column(name = "condition_code", nullable = false) private String conditionCode;
    @Column(name = "condition_notes") private String conditionNotes;
    @Column(name = "provenance_summary") private String provenanceSummary;
    @Column(name = "provenance_previous_owner") private String provenancePreviousOwner;
    @Column(name = "provenance_documentation_reference") private String provenanceDocumentationReference;
    private BigDecimal height, width, depth, weight;
    @Column(name = "length_value") private BigDecimal lengthValue;
    @Column(name = "dimension_unit") private String dimensionUnit;
    @Column(name = "weight_unit") private String weightUnit;
    @Column(name = "reserve_price") private BigDecimal reservePrice;
    @Column(nullable = false) private String currency;
    @Column(name = "buyer_premium_rate", nullable = false) private BigDecimal buyerPremiumRate;
    @Column(name = "tax_rate", nullable = false) private BigDecimal taxRate;
    @Column(name = "winning_customer_id") private String winningCustomerId;
    @Column(name = "winning_bid_amount") private BigDecimal winningBidAmount;
    @Column(name = "settled_at") private Instant settledAt;
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "lot_fulfillment_options", joinColumns = @JoinColumn(name = "lot_id"))
    @Column(name = "option_code") private Set<String> fulfillmentOptions = new LinkedHashSet<String>();
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "lot_images", joinColumns = @JoinColumn(name = "lot_id"))
    @Column(name = "image_url") private Set<String> imageUrls = new LinkedHashSet<String>();

    protected LotEntity() { }

    public LotEntity(String id, Instant closesAt) {
        this(id, null, closesAt);
    }
    public LotEntity(String id, String auctionId, Instant closesAt) {
        this.id = id; this.auctionId = auctionId; this.closesAt = closesAt; this.status = "OPEN"; this.version = 0L;
        this.conditionCode = "UNKNOWN"; this.currency = "USD"; this.buyerPremiumRate = BigDecimal.ZERO; this.taxRate = BigDecimal.ZERO;
    }
    public LotEntity(String auctionId, Lot lot) {
        this(lot.getId(), auctionId, lot.getClosesAt());
        this.opensAt = lot.getOpensAt(); this.category = lot.getCategory(); this.conditionCode = lot.getCondition().name(); this.conditionNotes = lot.getConditionNotes();
        if (lot.getProvenance() != null) { this.provenanceSummary = lot.getProvenance().getSummary(); this.provenancePreviousOwner = lot.getProvenance().getPreviousOwner(); this.provenanceDocumentationReference = lot.getProvenance().getDocumentationReference(); }
        if (lot.getMeasurements() != null) { LotMeasurements m = lot.getMeasurements(); this.height = m.getHeight(); this.width = m.getWidth(); this.lengthValue = m.getLength(); this.depth = m.getDepth(); this.weight = m.getWeight(); this.dimensionUnit = m.getDimensionUnit(); this.weightUnit = m.getWeightUnit(); }
        this.reservePrice = lot.getReservePrice(); this.currency = lot.getCurrency(); this.buyerPremiumRate = lot.getBuyerPremiumRate(); this.taxRate = lot.getTaxRate();
        for (FulfillmentOption option : lot.getFulfillmentOptions()) this.fulfillmentOptions.add(option.name()); this.imageUrls.addAll(lot.getImageUrls());
    }
    public String getId() { return id; }
    public Instant getClosesAt() { return closesAt; }
    public String getAuctionId() { return auctionId; }
    public String getStatus() { return status; }
    public BigDecimal getHighestBidAmount() { return highestBidAmount; }
    public String getHighestBidderId() { return highestBidderId; }
    public Long getVersion() { return version; }
    public void updateClosingTime(Instant newClosingTime) { this.closesAt = newClosingTime; }
    public void recordWinner(com.jennparker.auction.domain.Bid winner, Instant settledAt) {
        if (winner != null) { this.winningCustomerId = winner.getCustomerId(); this.winningBidAmount = winner.getAmount(); this.settledAt = settledAt; this.status = "CLOSED"; }
    }
    public com.jennparker.auction.domain.Lot toDomain() {
        Provenance provenance = provenanceSummary == null && provenancePreviousOwner == null && provenanceDocumentationReference == null ? null
                : new Provenance(provenanceSummary, provenancePreviousOwner, provenanceDocumentationReference);
        LotMeasurements measurements = height == null && width == null && lengthValue == null && depth == null && weight == null ? null
                : new LotMeasurements(height, width, lengthValue, depth, weight, dimensionUnit, weightUnit);
        Lot lot = new Lot(id, auctionId, opensAt, closesAt, category, LotCondition.valueOf(conditionCode), conditionNotes,
                provenance, measurements, enumOptions(), new ArrayList<String>(imageUrls), reservePrice, currency, buyerPremiumRate, taxRate);
        if (highestBidAmount != null && highestBidderId != null && highestBidAt != null) {
            lot.restoreHighestBid(new Bid(highestBidderId, highestBidAmount, highestBidAt, highestBidAcceptedAt));
        }
        return lot;
    }
    private List<FulfillmentOption> enumOptions() { List<FulfillmentOption> result = new ArrayList<FulfillmentOption>(); for (String option : fulfillmentOptions) result.add(FulfillmentOption.valueOf(option)); return result; }
}
