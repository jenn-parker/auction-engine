package com.jennparker.auction.api;

import com.jennparker.auction.application.AuctionService;
import com.jennparker.auction.application.SettlementPaymentPlan;
import com.jennparker.auction.domain.Bid;
import com.jennparker.auction.domain.Order;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import javax.validation.Valid;
import javax.validation.constraints.*;
import java.math.BigDecimal;
import java.net.URI;
import java.time.Instant;
import java.util.List;

@RestController
@RequestMapping("/api/v1")
@Tag(name = "Auction Operations", description = "Live bidding and internal settlement operations")
public class AuctionOperationsController {
    private final AuctionService auctions;
    private final com.jennparker.auction.application.AuctionManagementService auctionManagement;
    public AuctionOperationsController(AuctionService auctions, com.jennparker.auction.application.AuctionManagementService auctionManagement) {
        this.auctions = auctions; this.auctionManagement = auctionManagement;
    }

    @Operation(summary = "Submit a live bid", description = "Validates customer eligibility and atomically accepts a strictly higher bid before lot closure.")
    @PostMapping("/lots/{lotId}/bids")
    public ResponseEntity<Void> bid(@PathVariable String lotId, @Valid @RequestBody BidRequest request) {
        auctions.bid(lotId, new Bid(request.customerId, request.bidAmount, request.timestamp));
        return ResponseEntity.accepted().build();
    }

    @Operation(summary = "Settle a lot", description = "Closes a lot after validating pre-authorized payments and creates a settled order for its winning bid.")
    @PostMapping("/internal/lots/{lotId}/settle")
    public ResponseEntity<Order> settle(@PathVariable String lotId, @Valid @RequestBody SettlementRequest request) {
        Order order = auctions.settle(lotId, request.toPlan());
        return ResponseEntity.created(java.util.Objects.requireNonNull(URI.create("/api/v1/orders/" + order.getId()))).body(order);
    }

    @Operation(summary = "Settle an auction", description = "Settles the specified customer's winning lots after validating pre-authorized payments. The auction must already be CLOSED.")
    @PostMapping("/internal/auctions/{auctionId}/settle")
    public ResponseEntity<List<Order>> settleAuction(@PathVariable String auctionId, @Valid @RequestBody SettlementRequest request) {
        com.jennparker.auction.domain.Auction auction = auctionManagement.get(auctionId);
        List<Order> orders = auctions.settleAuction(auctionId, auction.getLots(), request.toPlan());
        return ResponseEntity.created(java.util.Objects.requireNonNull(URI.create("/api/v1/internal/auctions/" + auctionId + "/settlement"))).body(orders);
    }

    public static class BidRequest {
        @NotBlank public String customerId; @NotNull @DecimalMin("0.01") public BigDecimal bidAmount; @NotNull public Instant timestamp;
    }
    public static class SettlementRequest {
        @NotBlank public String customerId;
        @NotNull @Valid public List<PaymentRequest> payments = new java.util.ArrayList<PaymentRequest>();
        @DecimalMin("0.00") public BigDecimal creditAmount = BigDecimal.ZERO;
        SettlementPaymentPlan toPlan() {
            List<SettlementPaymentPlan.Payment> result = new java.util.ArrayList<SettlementPaymentPlan.Payment>();
            for (PaymentRequest payment : payments) result.add(new SettlementPaymentPlan.Payment(payment.paymentId, payment.paymentMethodId, payment.amount));
            return new SettlementPaymentPlan(customerId, result, creditAmount);
        }
    }
    public static class PaymentRequest {
        public String paymentId;
        public String paymentMethodId;
        @NotNull @DecimalMin("0.01") public BigDecimal amount;
        @AssertTrue(message = "paymentId or paymentMethodId is required")
        public boolean hasPaymentReference() { return (paymentId != null && !paymentId.trim().isEmpty()) || (paymentMethodId != null && !paymentMethodId.trim().isEmpty()); }
    }
}
