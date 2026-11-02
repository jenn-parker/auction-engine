package com.jennparker.auction.persistence;

import com.jennparker.auction.domain.Auctioneer;
import javax.persistence.*;

@Entity
@Table(name = "auctioneers")
public class AuctioneerEntity {
    @Id private String id;
    @Column(name = "cognito_subject", nullable = false, unique = true) private String cognitoSubject;
    @Column(nullable = false) private String name;
    @Column(nullable = false) private String email;
    @Column(nullable = false) private boolean active;
    protected AuctioneerEntity() { }
    public AuctioneerEntity(Auctioneer auctioneer) { this.id = auctioneer.getId(); this.cognitoSubject = auctioneer.getCognitoSubject(); this.name = auctioneer.getName(); this.email = auctioneer.getEmail(); this.active = auctioneer.isActive(); }
    public Auctioneer toDomain() { return new Auctioneer(id, cognitoSubject, name, email, active); }
}
