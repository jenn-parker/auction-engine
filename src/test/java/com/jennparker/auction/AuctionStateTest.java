package com.jennparker.auction;

import com.jennparker.auction.domain.Auction;
import com.jennparker.auction.domain.AuctionState;
import com.jennparker.auction.domain.Lot;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;

class AuctionStateTest {
    private static final Instant SCHEDULED = Instant.parse("2026-11-02T20:00:00Z");
    private static final Instant TRANSITIONED_AT = Instant.parse("2026-11-01T20:00:00Z");

    @Test
    void followsTheOnlyAllowedLifecycle() {
        Auction auction = new Auction("auction-1", SCHEDULED, null, Collections.<Lot>emptyList());

        auction = auction.transitionTo(AuctionState.UPCOMING, TRANSITIONED_AT);
        assertEquals(TRANSITIONED_AT, auction.getPublishedAt());
        auction = auction.transitionTo(AuctionState.LIVE, TRANSITIONED_AT.plusSeconds(60));
        assertEquals(AuctionState.LIVE, auction.getState());
        auction = auction.transitionTo(AuctionState.CLOSED, TRANSITIONED_AT.plusSeconds(120));

        assertEquals(AuctionState.CLOSED, auction.getState());
        assertEquals(TRANSITIONED_AT.plusSeconds(120), auction.getClosedAt());
    }

    @Test
    void rejectsSkippingOrReversingStates() {
        Auction auction = new Auction("auction-1", SCHEDULED, null, Collections.<Lot>emptyList());

        assertThrows(IllegalStateException.class, () -> auction.transitionTo(AuctionState.LIVE, TRANSITIONED_AT));
        Auction upcoming = auction.transitionTo(AuctionState.UPCOMING, TRANSITIONED_AT);
        assertThrows(IllegalStateException.class, () -> upcoming.transitionTo(AuctionState.NOT_YET_PUBLIC, TRANSITIONED_AT));
    }

    @Test
    void rejectsLotsBelongingToAnotherAuction() {
        Lot lot = new Lot("lot-1", "auction-2", SCHEDULED);

        assertThrows(IllegalArgumentException.class,
                () -> new Auction("auction-1", SCHEDULED, null, Collections.singletonList(lot)));
    }
}
