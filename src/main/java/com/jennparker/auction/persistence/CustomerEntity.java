package com.jennparker.auction.persistence;

import com.jennparker.auction.domain.*;
import javax.persistence.*;
import java.util.Collections;
import java.util.List;

@Entity
@Table(name = "customers")
public class CustomerEntity {
    @Id private String id;
    private String name;
    private String email;
    private String phone;
    @Column(name = "address_line1") private String addressLine1;
    @Column(name = "address_line2") private String addressLine2;
    @Column(name = "address_city") private String addressCity;
    @Column(name = "address_state_or_region") private String addressStateOrRegion;
    @Column(name = "address_postal_code") private String addressPostalCode;
    @Column(name = "address_country_code") private String addressCountryCode;
    @Column(name = "account_status", nullable = false) private String accountStatus;
    @Column(name = "bidding_enabled", nullable = false) private boolean biddingEnabled;

    protected CustomerEntity() { }

    public CustomerEntity(Customer customer) { updateFrom(customer); this.id = customer.getId(); }

    public void updateFrom(Customer customer) {
        this.name = customer.getName(); this.email = customer.getEmail(); this.phone = customer.getPhone();
        Address address = customer.getAddress();
        this.addressLine1 = address == null ? null : address.getLine1();
        this.addressLine2 = address == null ? null : address.getLine2();
        this.addressCity = address == null ? null : address.getCity();
        this.addressStateOrRegion = address == null ? null : address.getStateOrRegion();
        this.addressPostalCode = address == null ? null : address.getPostalCode();
        this.addressCountryCode = address == null ? null : address.getCountryCode();
        this.accountStatus = customer.getAccountStatus().name(); this.biddingEnabled = customer.isBiddingEnabled();
    }

    public Customer toDomain(List<PaymentMethod> paymentMethods) {
        Address address = addressLine1 == null && addressCity == null && addressPostalCode == null ? null
                : new Address(addressLine1, addressLine2, addressCity, addressStateOrRegion, addressPostalCode, addressCountryCode);
        return new Customer(id, name, email, phone, address, Customer.AccountStatus.valueOf(accountStatus), biddingEnabled, paymentMethods);
    }
    public String getId() { return id; }
    public String getName() { return name; }
    public String getEmail() { return email; }
    public String getPhone() { return phone; }
    public String getAccountStatus() { return accountStatus; }
    public boolean isBiddingEnabled() { return biddingEnabled; }
}
