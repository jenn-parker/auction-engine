package com.jennparker.auction.application;

import com.jennparker.auction.domain.Customer;
import com.jennparker.auction.persistence.CustomerEntity;
import com.jennparker.auction.persistence.CustomerRepository;
import com.jennparker.auction.persistence.PaymentMethodEntity;
import com.jennparker.auction.persistence.PaymentMethodRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class CustomerManagementService {
    private final CustomerRepository customers;
    private final PaymentMethodRepository paymentMethods;
    public CustomerManagementService(CustomerRepository customers, PaymentMethodRepository paymentMethods) {
        this.customers = customers; this.paymentMethods = paymentMethods;
    }

    @Transactional
    public Customer create(Customer customer) {
        if (customers.existsById(customer.getId())) throw new DuplicateCustomerException();
        CustomerEntity entity = customers.save(new CustomerEntity(customer));
        savePaymentMethods(customer);
        return toDomain(entity);
    }

    @Transactional
    public Customer update(Customer customer) {
        CustomerEntity entity = customers.findById(customer.getId()).orElseThrow(CustomerNotFoundException::new);
        entity.updateFrom(customer);
        CustomerEntity saved = customers.save(entity);
        savePaymentMethods(customer);
        return toDomain(saved);
    }

    @Transactional(readOnly = true)
    public Customer get(String customerId) {
        return customers.findById(customerId).map(this::toDomain).orElseThrow(CustomerNotFoundException::new);
    }

    @Transactional
    public com.jennparker.auction.domain.PaymentMethod addPaymentMethod(String customerId, com.jennparker.auction.domain.PaymentMethod method) {
        if (!customers.existsById(customerId)) throw new CustomerNotFoundException();
        if (paymentMethods.existsById(method.getId())) throw new DuplicatePaymentMethodException();
        if (method.isDefault()) clearDefault(customerId, null);
        return paymentMethods.save(new PaymentMethodEntity(customerId, method)).toDomain();
    }

    @Transactional
    public com.jennparker.auction.domain.PaymentMethod updatePaymentMethod(String customerId, String methodId, com.jennparker.auction.domain.PaymentMethod method) {
        PaymentMethodEntity entity = paymentMethods.findByIdAndCustomerId(methodId, customerId).orElseThrow(PaymentMethodNotFoundException::new);
        if (method.isDefault()) clearDefault(customerId, methodId);
        entity.update(method);
        return paymentMethods.save(entity).toDomain();
    }

    @Transactional
    public void removePaymentMethod(String customerId, String methodId) {
        PaymentMethodEntity entity = paymentMethods.findByIdAndCustomerId(methodId, customerId).orElseThrow(PaymentMethodNotFoundException::new);
        paymentMethods.delete(entity);
    }

    private void savePaymentMethods(Customer customer) {
        for (com.jennparker.auction.domain.PaymentMethod method : customer.getPaymentMethods()) {
            if (method.isDefault()) clearDefault(customer.getId(), method.getId());
            paymentMethods.save(new PaymentMethodEntity(customer.getId(), method));
        }
    }
    private void clearDefault(String customerId, String exceptId) {
        boolean changed = false;
        for (PaymentMethodEntity existing : paymentMethods.findAllByCustomerId(customerId)) {
            if (exceptId == null || !exceptId.equals(existing.getId())) {
                existing.clearDefault();
                paymentMethods.save(existing);
                changed = true;
            }
        }
        if (changed) paymentMethods.flush();
    }
    private Customer toDomain(CustomerEntity entity) {
        List<com.jennparker.auction.domain.PaymentMethod> methods = paymentMethods.findAllByCustomerId(entity.getId()).stream()
                .map(PaymentMethodEntity::toDomain).collect(Collectors.toList());
        return entity.toDomain(methods);
    }
    public static class DuplicateCustomerException extends RuntimeException { }
    public static class CustomerNotFoundException extends RuntimeException { }
    public static class DuplicatePaymentMethodException extends RuntimeException { }
    public static class PaymentMethodNotFoundException extends RuntimeException { }
}
