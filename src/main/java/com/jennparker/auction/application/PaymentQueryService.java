package com.jennparker.auction.application;

import com.jennparker.auction.domain.Payment;
import com.jennparker.auction.persistence.CustomerRepository;
import com.jennparker.auction.persistence.PaymentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class PaymentQueryService {
    private final CustomerRepository customers;
    private final PaymentRepository payments;
    public PaymentQueryService(CustomerRepository customers, PaymentRepository payments) { this.customers = customers; this.payments = payments; }

    @Transactional(readOnly = true)
    public List<Payment> list(String customerId) {
        requireCustomer(customerId);
        return payments.findAllByCustomerIdOrderByCreatedAtDesc(customerId).stream().map(e -> e.toDomain()).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public Payment get(String customerId, String paymentId) {
        requireCustomer(customerId);
        try { return payments.findByIdAndCustomerId(UUID.fromString(paymentId), customerId).map(e -> e.toDomain()).orElseThrow(PaymentNotFoundException::new); }
        catch (IllegalArgumentException ex) { throw new PaymentNotFoundException(); }
    }

    @Transactional(readOnly = true)
    public Payment getById(String paymentId) {
        try { return payments.findById(UUID.fromString(paymentId)).map(e -> e.toDomain()).orElseThrow(PaymentNotFoundException::new); }
        catch (IllegalArgumentException ex) { throw new PaymentNotFoundException(); }
    }

    private void requireCustomer(String customerId) {
        if (!customers.existsById(customerId)) throw new CustomerManagementService.CustomerNotFoundException();
    }
    public static class PaymentNotFoundException extends RuntimeException { }
}
