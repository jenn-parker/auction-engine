package com.jennparker.auction;

import com.jennparker.auction.application.AuctionService;
import com.jennparker.auction.application.AuctionStateProvider;
import com.jennparker.auction.domain.*;
import com.jennparker.auction.persistence.LotRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class AuctionServiceTest {
    private static final Instant NOW = Instant.parse("2026-11-01T21:00:00Z");
    private AuctionService service;
    private AuctionStateProvider states;
    private CustomerService customers;
    private LotRepository lots;

    @BeforeEach
    void setUp() {
        states = mock(AuctionStateProvider.class);
        customers = mock(CustomerService.class);
        lots = mock(LotRepository.class);
        service = new AuctionService(customers, Clock.fixed(NOW, ZoneOffset.UTC), states, lots);
    }

    @Test
    void rejectsBidsWhenAuctionIsNotLive() {
        Lot lot = new Lot("lot-1", "auction-1", NOW.plusSeconds(60));
        service.register(lot);
        when(states.stateOf("auction-1")).thenReturn(AuctionState.UPCOMING);

        assertThrows(AuctionService.InvalidBidException.class,
                () -> service.bid("lot-1", new Bid("cust-1", BigDecimal.TEN, NOW)));
        verifyNoInteractions(customers);
    }

    @Test
    void rejectsIneligibleCustomersBeforeMutatingTheLot() {
        Lot lot = new Lot("lot-1", "auction-1", NOW.plusSeconds(60));
        service.register(lot);
        when(states.stateOf("auction-1")).thenReturn(AuctionState.LIVE);
        when(customers.verify("cust-1")).thenReturn(new CustomerEligibility(true, false, true));

        assertThrows(AuctionService.ForbiddenException.class,
                () -> service.bid("lot-1", new Bid("cust-1", BigDecimal.TEN, NOW)));
        assertNull(lot.getHighestBid());
    }

    @Test
    void settlesTheWinningBidIntoAPendingOrder() {
        Lot lot = new Lot("lot-1", "auction-1", NOW);
        Clock advancingClock = mock(Clock.class);
        when(advancingClock.instant()).thenReturn(NOW.minusSeconds(1), NOW.plusSeconds(1));
        service = new AuctionService(customers, advancingClock, states, lots);
        service.register(lot);
        when(states.stateOf("auction-1")).thenReturn(AuctionState.LIVE, AuctionState.CLOSED);
        when(customers.verify("cust-1")).thenReturn(new CustomerEligibility(true, true, true));
        when(lots.findById("lot-1")).thenReturn(Optional.empty());

        service.bid("lot-1", new Bid("cust-1", BigDecimal.valueOf(125), NOW.minusSeconds(10)));
        Order order = service.settle("lot-1");

        assertEquals(Order.Status.PENDING, order.getStatus());
        assertEquals("cust-1", order.getCustomerId());
        assertEquals(BigDecimal.valueOf(125), order.getAmount());
        assertEquals(Lot.Status.CLOSED, lot.getStatus());
        verify(lots).findById("lot-1");
    }
}
