package com.jennparker.auction.application;

import com.jennparker.auction.domain.Auctioneer;
import com.jennparker.auction.persistence.AuctioneerRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuctioneerAuthorizationService {
    private final AuctioneerRepository auctioneers;
    public AuctioneerAuthorizationService(AuctioneerRepository auctioneers) { this.auctioneers = auctioneers; }

    @Transactional(readOnly = true)
    public Auctioneer requireActive(String cognitoSubject) {
        if (cognitoSubject == null) throw new AuctioneerUnauthorizedException();
        Auctioneer auctioneer = auctioneers.findByCognitoSubject(cognitoSubject).map(e -> e.toDomain()).orElseThrow(AuctioneerUnauthorizedException::new);
        if (!auctioneer.isActive()) throw new AuctioneerUnauthorizedException();
        return auctioneer;
    }

    @Transactional
    public Auctioneer provision(Auctioneer auctioneer) {
        if (auctioneers.existsById(auctioneer.getId()) || auctioneers.findByCognitoSubject(auctioneer.getCognitoSubject()).isPresent()) throw new AuctioneerConflictException();
        return auctioneers.save(new com.jennparker.auction.persistence.AuctioneerEntity(auctioneer)).toDomain();
    }
    public static class AuctioneerUnauthorizedException extends RuntimeException { }
    public static class AuctioneerConflictException extends RuntimeException { }
}
