package com.jennparker.auction.api;

import com.jennparker.auction.application.AuctionManagementService;
import com.jennparker.auction.application.AuctionService;
import com.jennparker.auction.domain.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import javax.validation.Valid;
import javax.validation.constraints.*;
import java.net.URI;
import java.time.Instant;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;

@RestController
@RequestMapping("/api/v1")
@Tag(name = "Auction Management", description = "Create and manage auctions and their lots")
public class AuctionManagementController {
    private final AuctionManagementService auctions;
    private final AuctionService auctionService;
    private final com.jennparker.auction.application.AuctioneerAuthorizationService auctioneerAuthorization;
    private final boolean cognitoEnabled;
    public AuctionManagementController(AuctionManagementService auctions, AuctionService auctionService, com.jennparker.auction.application.AuctioneerAuthorizationService auctioneerAuthorization,
                                       @Value("${security.cognito.enabled:false}") boolean cognitoEnabled) {
        this.auctions = auctions; this.auctionService = auctionService; this.auctioneerAuthorization = auctioneerAuthorization; this.cognitoEnabled = cognitoEnabled;
    }

    @Operation(summary = "Create an auction", description = "Creates a scheduled auction with an optional physical venue.")
    @PostMapping("/auctions")
    public ResponseEntity<Auction> createAuction(@Valid @RequestBody CreateAuctionRequest request, @AuthenticationPrincipal Jwt jwt) {
        authorizeAuctioneer(jwt);
        Auction auction = auctions.create(request.toDomain());
        return ResponseEntity.created(java.util.Objects.requireNonNull(URI.create("/api/v1/auctions/" + auction.getId()))).body(auction);
    }

    @Operation(summary = "Get an auction", description = "Returns an auction and all lots assigned to it.")
    @GetMapping("/auctions/{auctionId}")
    public Auction getAuction(@PathVariable String auctionId) { return auctions.get(auctionId); }

    @Operation(summary = "List auction lots", description = "Returns all lots assigned to an auction, including current bid and catalog details.")
    @GetMapping("/auctions/{auctionId}/lots")
    public List<Lot> getAuctionLots(@PathVariable String auctionId) { return auctions.getLots(auctionId); }

    @Operation(summary = "Get a lot", description = "Returns a lot's catalog details, bidding state, and settlement state.")
    @GetMapping("/lots/{lotId}")
    public Lot getLot(@PathVariable String lotId) { return auctions.getLot(lotId); }

    @Operation(summary = "Publish an auction", description = "Moves an auction from NOT_YET_PUBLIC to UPCOMING.")
    @PostMapping("/auctions/{auctionId}/publish")
    public Auction publishAuction(@PathVariable String auctionId, @AuthenticationPrincipal Jwt jwt) { authorizeAuctioneer(jwt); return auctions.transition(auctionId, AuctionState.UPCOMING); }

    @Operation(summary = "Start an auction", description = "Moves an upcoming auction to LIVE so it can accept bids.")
    @PostMapping("/auctions/{auctionId}/start")
    public Auction startAuction(@PathVariable String auctionId, @AuthenticationPrincipal Jwt jwt) { authorizeAuctioneer(jwt); return auctions.transition(auctionId, AuctionState.LIVE); }

    @Operation(summary = "Close an auction", description = "Moves a live auction to CLOSED and prevents new bids.")
    @PostMapping("/auctions/{auctionId}/close")
    public Auction closeAuction(@PathVariable String auctionId, @AuthenticationPrincipal Jwt jwt) { authorizeAuctioneer(jwt); return auctions.transition(auctionId, AuctionState.CLOSED); }

    @Operation(summary = "Replace an auction", description = "Replaces the auction schedule and physical location. Lots are unchanged.")
    @PutMapping("/auctions/{auctionId}")
    public Auction updateAuction(@PathVariable String auctionId, @Valid @RequestBody AuctionRequest request, @AuthenticationPrincipal Jwt jwt) {
        authorizeAuctioneer(jwt);
        return auctions.update(request.toDomain(auctionId));
    }

    @Operation(summary = "Partially update an auction", description = "Updates only supplied auction fields and preserves omitted values.")
    @PatchMapping("/auctions/{auctionId}")
    public Auction patchAuction(@PathVariable String auctionId, @RequestBody AuctionPatchRequest request, @AuthenticationPrincipal Jwt jwt) {
        authorizeAuctioneer(jwt);
        Auction current = auctions.get(auctionId);
        return auctions.update(request.apply(current));
    }

    @Operation(summary = "Create a lot in an auction", description = "Creates a lot owned by exactly one auction.")
    @PostMapping("/auctions/{auctionId}/lots")
    public ResponseEntity<Lot> createAuctionLot(@PathVariable String auctionId, @Valid @RequestBody CreateLotRequest request, @AuthenticationPrincipal Jwt jwt) {
        authorizeAuctioneer(jwt);
        Lot lot = auctions.addLot(auctionId, request.toLot(auctionId));
        auctionService.register(lot);
        return ResponseEntity.created(java.util.Objects.requireNonNull(URI.create("/api/v1/lots/" + lot.getId()))).body(lot);
    }

    @Operation(summary = "Create a lot using the compatibility path", description = "Creates a lot only when auctionId is supplied. Prefer the auction-scoped endpoint.")
    @PostMapping("/lots")
    public ResponseEntity<Lot> createLot(@Valid @RequestBody CreateLotRequest request, @AuthenticationPrincipal Jwt jwt) {
        authorizeAuctioneer(jwt);
        if (request.auctionId == null || request.auctionId.trim().isEmpty()) throw new AuctionRequiredException();
        Lot lot = auctions.addLot(request.auctionId, request.toLot(request.auctionId));
        auctionService.register(lot);
        return ResponseEntity.created(java.util.Objects.requireNonNull(URI.create("/api/v1/lots/" + lot.getId()))).body(lot);
    }

    @Operation(summary = "Replace a lot", description = "Replaces the mutable lot closing time. A lot cannot be moved to another auction.")
    @PutMapping("/lots/{lotId}")
    public Lot updateLot(@PathVariable String lotId, @Valid @RequestBody LotUpdateRequest request, @AuthenticationPrincipal Jwt jwt) {
        authorizeAuctioneer(jwt);
        return auctions.updateLot(lotId, request.closesAt);
    }

    @Operation(summary = "Partially update a lot", description = "Updates the lot closing time while preserving auction ownership.")
    @PatchMapping("/lots/{lotId}")
    public Lot patchLot(@PathVariable String lotId, @Valid @RequestBody LotPatchRequest request, @AuthenticationPrincipal Jwt jwt) {
        authorizeAuctioneer(jwt);
        return auctions.updateLot(lotId, request.closesAt);
    }

    @Operation(summary = "Delete a lot", description = "Deletes a lot when no bids or orders reference it.")
    @DeleteMapping("/lots/{lotId}")
    @ResponseStatus(org.springframework.http.HttpStatus.NO_CONTENT)
    public void deleteLot(@PathVariable String lotId, @AuthenticationPrincipal Jwt jwt) { authorizeAuctioneer(jwt); auctions.deleteLot(lotId); auctionService.remove(lotId); }

    private void authorizeAuctioneer(Jwt jwt) {
        if (cognitoEnabled) auctioneerAuthorization.requireActive(jwt == null ? null : jwt.getSubject());
    }

    public static class CreateAuctionRequest {
        @NotBlank public String auctionId; @NotNull public Instant scheduledAt;
        @Valid public PhysicalLocationRequest physicalLocation;
        Auction toDomain() { return new Auction(auctionId, scheduledAt, location(), new ArrayList<Lot>()); }
        private PhysicalLocation location() { return physicalLocation == null ? null : physicalLocation.toDomain(); }
    }
    public static class AuctionRequest {
        @NotNull public Instant scheduledAt; @Valid public PhysicalLocationRequest physicalLocation;
        Auction toDomain(String id) { return new Auction(id, scheduledAt, physicalLocation == null ? null : physicalLocation.toDomain(), new ArrayList<Lot>()); }
    }
    public static class AuctionPatchRequest {
        public Instant scheduledAt; @Valid public PhysicalLocationRequest physicalLocation;
        Auction apply(Auction current) { return new Auction(current.getId(), scheduledAt == null ? current.getScheduledAt() : scheduledAt,
                physicalLocation == null ? current.getPhysicalLocation() : physicalLocation.toDomain(), current.getLots()); }
    }
    public static class CreateLotRequest {
        @NotBlank public String lotId; public Instant opensAt; @NotNull public Instant closesAt; public String auctionId;
        public String category; public LotCondition condition = LotCondition.UNKNOWN; public String conditionNotes;
        @Valid public ProvenanceRequest provenance; @Valid public MeasurementsRequest measurements;
        public List<FulfillmentOption> fulfillmentOptions = new ArrayList<FulfillmentOption>();
        public List<String> imageUrls = new ArrayList<String>();
        @DecimalMin("0.00") public BigDecimal reservePrice;
        @Size(min = 3, max = 3) public String currency = "USD";
        @DecimalMin("0.00") public BigDecimal buyerPremiumRate = BigDecimal.ZERO;
        @DecimalMin("0.00") public BigDecimal taxRate = BigDecimal.ZERO;
        Lot toLot(String auctionId) {
            Provenance p = provenance == null ? null : new Provenance(provenance.summary, provenance.previousOwner, provenance.documentationReference);
            LotMeasurements m = measurements == null ? null : new LotMeasurements(measurements.height, measurements.width, measurements.length, measurements.depth, measurements.weight, measurements.dimensionUnit, measurements.weightUnit);
            return new Lot(lotId, auctionId, opensAt, closesAt, category, condition, conditionNotes, p, m, fulfillmentOptions, imageUrls, reservePrice, currency, buyerPremiumRate, taxRate);
        }
    }
    public static class ProvenanceRequest { public String summary, previousOwner, documentationReference; }
    public static class MeasurementsRequest {
        public BigDecimal height, width, length, depth, weight; public String dimensionUnit, weightUnit;
    }
    public static class LotUpdateRequest { @NotNull public Instant closesAt; }
    public static class LotPatchRequest { @NotNull public Instant closesAt; }
    public static class PhysicalLocationRequest {
        public String venueName, line1, line2, city, stateOrRegion, postalCode;
        @Size(min = 2, max = 2) public String countryCode;
        PhysicalLocation toDomain() { return new PhysicalLocation(venueName, line1, line2, city, stateOrRegion, postalCode, countryCode); }
    }
    public static class AuctionRequiredException extends RuntimeException { }
}
