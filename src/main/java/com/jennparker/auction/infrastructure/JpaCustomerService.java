package com.jennparker.auction.infrastructure;

import com.jennparker.auction.domain.Customer;
import com.jennparker.auction.domain.CustomerService;
import com.jennparker.auction.persistence.CustomerRepository;
import com.jennparker.auction.persistence.PaymentMethodEntity;
import com.jennparker.auction.persistence.PaymentMethodRepository;
import org.springframework.stereotype.Service;
import java.util.stream.Collectors;

@Service
public class JpaCustomerService implements CustomerService {
    private final CustomerRepository customers;
    private final PaymentMethodRepository paymentMethods;
    public JpaCustomerService(CustomerRepository customers, PaymentMethodRepository paymentMethods) {
        this.customers = customers; this.paymentMethods = paymentMethods;
    }
    @Override public Customer find(String customerId) {
        return customers.findById(customerId).map(entity -> entity.toDomain(
                paymentMethods.findAllByCustomerId(customerId).stream().map(PaymentMethodEntity::toDomain).collect(Collectors.toList()))).orElse(null);
    }
}
