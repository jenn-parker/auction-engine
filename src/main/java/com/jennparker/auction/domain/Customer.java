package com.jennparker.auction.domain;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Immutable customer snapshot used by bid authorization. */
public final class Customer {
    private final String id;
    private final String name;
    private final String email;
    private final String phone;
    private final Address address;
    private final AccountStatus accountStatus;
    private final boolean biddingEnabled;
    private final List<PaymentMethod> paymentMethods;

    public Customer(String id, AccountStatus accountStatus, boolean biddingEnabled, PaymentMethod paymentMethod) {
        this(id, null, null, null, null, accountStatus, biddingEnabled,
                paymentMethod == null ? Collections.<PaymentMethod>emptyList() : Collections.singletonList(paymentMethod));
    }

    public Customer(String id, String name, String email, String phone, Address address,
                    AccountStatus accountStatus, boolean biddingEnabled, PaymentMethod paymentMethod) {
        this(id, name, email, phone, address, accountStatus, biddingEnabled,
                paymentMethod == null ? Collections.<PaymentMethod>emptyList() : Collections.singletonList(paymentMethod));
    }

    public Customer(String id, String name, String email, String phone, Address address,
                    AccountStatus accountStatus, boolean biddingEnabled, List<PaymentMethod> paymentMethods) {
        if (id == null || accountStatus == null) throw new IllegalArgumentException("Customer id and status are required");
        this.id = id;
        this.name = name;
        this.email = email;
        this.phone = phone;
        this.address = address;
        this.accountStatus = accountStatus;
        this.biddingEnabled = biddingEnabled;
        this.paymentMethods = Collections.unmodifiableList(new ArrayList<PaymentMethod>(paymentMethods == null
                ? Collections.<PaymentMethod>emptyList() : paymentMethods));
    }

    public String getId() { return id; }
    public String getName() { return name; }
    public String getEmail() { return email; }
    public String getPhone() { return phone; }
    public Address getAddress() { return address; }
    public AccountStatus getAccountStatus() { return accountStatus; }
    public boolean isBiddingEnabled() { return biddingEnabled; }
    /** Backward-compatible primary method accessor. */
    public PaymentMethod getPaymentMethod() { return paymentMethods.isEmpty() ? null : paymentMethods.get(0); }
    public List<PaymentMethod> getPaymentMethods() { return paymentMethods; }
    public boolean isEligibleToBid() {
        return accountStatus == AccountStatus.ACTIVE && biddingEnabled
                && paymentMethods.stream().anyMatch(PaymentMethod::isAuthorized);
    }

    public enum AccountStatus { ACTIVE, SUSPENDED, CLOSED }
}
