package com.jennparker.auction.api;

import com.jennparker.auction.application.AuctioneerAuthorizationService;
import com.jennparker.auction.domain.Auctioneer;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import javax.validation.Valid;
import javax.validation.constraints.Email;
import javax.validation.constraints.NotBlank;
import java.net.URI;

@RestController
@RequestMapping("/api/v1/internal/auctioneers")
@Tag(name = "Auctioneers", description = "Internal auctioneer provisioning")
public class AuctioneerController {
    private final AuctioneerAuthorizationService auctioneers;
    public AuctioneerController(AuctioneerAuthorizationService auctioneers) { this.auctioneers = auctioneers; }
    @Operation(summary = "Provision an auctioneer", description = "Creates an active auctioneer record linked to a Cognito user-pool subject.")
    @PostMapping
    public ResponseEntity<Auctioneer> provision(@Valid @RequestBody ProvisionAuctioneerRequest request) {
        Auctioneer auctioneer = auctioneers.provision(new Auctioneer(request.auctioneerId, request.cognitoSubject, request.name, request.email, true));
        return ResponseEntity.created(java.util.Objects.requireNonNull(URI.create("/api/v1/internal/auctioneers/" + auctioneer.getId()))).body(auctioneer);
    }
    public static class ProvisionAuctioneerRequest {
        @NotBlank public String auctioneerId; @NotBlank public String cognitoSubject; @NotBlank public String name; @NotBlank @Email public String email;
    }
}
