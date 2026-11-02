package com.jennparker.auction.domain;

public interface CustomerService {
    Customer find(String customerId);

    default CustomerEligibility verify(String customerId) {
        Customer customer = find(customerId);
        return new CustomerEligibility(customer != null && customer.getAccountStatus() == Customer.AccountStatus.ACTIVE,
                customer != null && customer.isBiddingEnabled(),
                customer != null && customer.getPaymentMethod() != null && customer.getPaymentMethod().isAuthorized());
    }
}
