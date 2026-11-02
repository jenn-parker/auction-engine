package com.jennparker.auction;

import com.jennparker.auction.application.AuctionService;
import com.jennparker.auction.application.AuctionStateProvider;
import com.jennparker.auction.domain.*;
import com.jennparker.auction.persistence.BidRepository;
import com.jennparker.auction.persistence.LotEntity;
import com.jennparker.auction.persistence.LotRepository;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class TransactionalBidPersistenceTest {
    private static final Instant NOW = Instant.parse("2026-11-01T21:00:00Z");

    @Test
    void recordsBidHistoryOnlyAfterDatabaseCompareAndSetSucceeds() {
        AuctionStateProvider states = mock(AuctionStateProvider.class);
        CustomerService customers = mock(CustomerService.class);
        LotRepository lots = mock(LotRepository.class);
        BidRepository bids = mock(BidRepository.class);
        Lot lot = new Lot("lot-1", "auction-1", NOW.plusSeconds(60));
        LotEntity persisted = new LotEntity("auction-1", lot);
        AuctionService service = new AuctionService(customers, Clock.fixed(NOW, ZoneOffset.UTC), states, lots, bids, null);
        service.register(lot);
        when(states.stateOf("auction-1")).thenReturn(AuctionState.LIVE);
        when(customers.verify("cust-1")).thenReturn(new CustomerEligibility(true, true, true));
        when(lots.acceptHigherBid(eq("lot-1"), eq(BigDecimal.TEN), eq("cust-1"), any(Instant.class))).thenReturn(1);
        when(lots.findById("lot-1")).thenReturn(Optional.of(persisted));

        service.bid("lot-1", new Bid("cust-1", BigDecimal.TEN, NOW.minusSeconds(1)));

        verify(lots).acceptHigherBid("lot-1", BigDecimal.TEN, "cust-1", NOW.minusSeconds(1));
        verify(bids).save(any());
    }

    @Test
    void doesNotWriteBidHistoryWhenAnotherBidWinsTheDatabaseRace() {
        AuctionStateProvider states = mock(AuctionStateProvider.class);
        CustomerService customers = mock(CustomerService.class);
        LotRepository lots = mock(LotRepository.class);
        BidRepository bids = mock(BidRepository.class);
        Lot lot = new Lot("lot-1", "auction-1", NOW.plusSeconds(60));
        AuctionService service = new AuctionService(customers, Clock.fixed(NOW, ZoneOffset.UTC), states, lots, bids, null);
        service.register(lot);
        when(states.stateOf("auction-1")).thenReturn(AuctionState.LIVE);
        when(customers.verify("cust-1")).thenReturn(new CustomerEligibility(true, true, true));
        when(lots.acceptHigherBid(anyString(), any(BigDecimal.class), anyString(), any(Instant.class))).thenReturn(0);

        assertThrows(AuctionService.InvalidBidException.class,
                () -> service.bid("lot-1", new Bid("cust-1", BigDecimal.TEN, NOW.minusSeconds(1))));
        verifyNoInteractions(bids);
    }
}
