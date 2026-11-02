package com.jennparker.auction.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface AuctioneerRepository extends JpaRepository<AuctioneerEntity, String> {
    Optional<AuctioneerEntity> findByCognitoSubject(String cognitoSubject);
}
