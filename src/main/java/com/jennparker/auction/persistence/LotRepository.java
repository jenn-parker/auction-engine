package com.jennparker.auction.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.math.BigDecimal;
import java.time.Instant;

public interface LotRepository extends JpaRepository<LotEntity, String> {
    List<LotEntity> findAllByAuctionId(String auctionId);

    /**
     * Database-level compare-and-set. A successful row count is the sole
     * authority for whether this bid won the race across all app instances.
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(value = "WITH database_clock AS (SELECT clock_timestamp() AS accepted_at) " +
            "UPDATE lots l SET highest_bid_amount = :amount, highest_bidder_id = :bidderId, highest_bid_at = :placedAt, " +
            "highest_bid_accepted_at = database_clock.accepted_at, version = l.version + 1 " +
            "FROM database_clock WHERE l.id = :lotId AND l.status = 'OPEN' " +
            "AND l.closes_at > database_clock.accepted_at AND l.closes_at > :placedAt " +
            "AND (l.highest_bid_amount IS NULL OR l.highest_bid_amount < :amount)", nativeQuery = true)
    int acceptHigherBid(@Param("lotId") String lotId, @Param("amount") BigDecimal amount,
                        @Param("bidderId") String bidderId, @Param("placedAt") Instant placedAt);
}
