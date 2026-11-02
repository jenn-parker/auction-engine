package com.jennparker.auction.application;

import com.jennparker.auction.domain.Payment;
import com.jennparker.auction.domain.PaymentStatus;
import com.jennparker.auction.persistence.CustomerRepository;
import com.jennparker.auction.persistence.PaymentEntity;
import com.jennparker.auction.persistence.PaymentMethodEntity;
import com.jennparker.auction.persistence.PaymentMethodRepository;
import com.jennparker.auction.persistence.PaymentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

/** Creates independently authorized payment transactions for later settlement application. */
@Service
public class PaymentService {
    private final CustomerRepository customers;
    private final PaymentMethodRepository paymentMethods;
    private final PaymentRepository payments;
    private final PaymentGateway gateway;

    public PaymentService(CustomerRepository customers, PaymentMethodRepository paymentMethods,
                          PaymentRepository payments, PaymentGateway gateway) {
        this.customers = customers; this.paymentMethods = paymentMethods; this.payments = payments; this.gateway = gateway;
    }

    @Transactional(noRollbackFor = PaymentAuthorizationException.class)
    public Payment create(String customerId, String paymentMethodId, BigDecimal amount, String idempotencyKey) {
        if (amount == null || amount.signum() <= 0 || idempotencyKey == null || idempotencyKey.trim().isEmpty()) {
            throw new InvalidPaymentException();
        }
        if (!customers.existsById(customerId)) throw new CustomerManagementService.CustomerNotFoundException();
        PaymentMethodEntity method = paymentMethods.findByIdAndCustomerId(paymentMethodId, customerId)
                .orElseThrow(CustomerManagementService.PaymentMethodNotFoundException::new);
        if (!method.isAuthorized()) throw new PaymentConflictException();
        java.util.Optional<PaymentEntity> existing = payments.findByIdempotencyKeyAndCustomerId(idempotencyKey, customerId);
        if (existing.isPresent()) return existing.get().toDomain();

        PaymentEntity entity = payments.save(new PaymentEntity(customerId, paymentMethodId, amount, PaymentStatus.PROCESSING.name(), idempotencyKey));
        Payment payment = entity.toDomain();
        try {
            gateway.authorize(payment, idempotencyKey);
            entity.updateStatus(PaymentStatus.AUTHORIZED.name());
            return payments.save(entity).toDomain();
        } catch (RuntimeException ex) {
            entity.updateStatus(PaymentStatus.FAILED.name());
            payments.save(entity);
            throw new PaymentAuthorizationException();
        }
    }

    public static class InvalidPaymentException extends RuntimeException { }
    public static class PaymentConflictException extends RuntimeException { }
    public static class PaymentAuthorizationException extends RuntimeException { }
}
