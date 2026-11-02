package com.jennparker.auction.application;

import com.jennparker.auction.domain.AuctionState;

public interface AuctionStateProvider {
    AuctionState stateOf(String auctionId);
}
