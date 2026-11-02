package com.jennparker.auction.domain;

public final class CustomerEligibility {
    private final boolean accountHealthy;
    private final boolean biddingEnabled;
    private final boolean paymentAuthorized;

    public CustomerEligibility(boolean accountHealthy, boolean biddingEnabled, boolean paymentAuthorized) {
        this.accountHealthy = accountHealthy;
        this.biddingEnabled = biddingEnabled;
        this.paymentAuthorized = paymentAuthorized;
    }
    public boolean isEligible() { return accountHealthy && biddingEnabled && paymentAuthorized; }
}
