package com.jennparker.auction;

import com.jennparker.auction.application.PaymentGateway;
import com.jennparker.auction.application.PaymentService;
import com.jennparker.auction.persistence.CustomerRepository;
import com.jennparker.auction.persistence.PaymentEntity;
import com.jennparker.auction.persistence.PaymentMethodEntity;
import com.jennparker.auction.persistence.PaymentMethodRepository;
import com.jennparker.auction.persistence.PaymentRepository;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class PaymentServiceTest {
    @Test
    void authorizesAndPersistsAPayment() {
        CustomerRepository customers = mock(CustomerRepository.class);
        PaymentMethodRepository methods = mock(PaymentMethodRepository.class);
        PaymentRepository payments = mock(PaymentRepository.class);
        PaymentGateway gateway = mock(PaymentGateway.class);
        PaymentMethodEntity method = mock(PaymentMethodEntity.class);
        PaymentEntity entity = new PaymentEntity("cust-1", "pm-1", BigDecimal.TEN, "PROCESSING", "key-1");
        when(customers.existsById("cust-1")).thenReturn(true);
        when(methods.findByIdAndCustomerId("pm-1", "cust-1")).thenReturn(Optional.of(method));
        when(method.isAuthorized()).thenReturn(true);
        when(payments.findByIdempotencyKeyAndCustomerId("key-1", "cust-1")).thenReturn(Optional.empty());
        when(payments.save(any(PaymentEntity.class))).thenReturn(entity);

        PaymentService service = new PaymentService(customers, methods, payments, gateway);
        assertEquals("AUTHORIZED", service.create("cust-1", "pm-1", BigDecimal.TEN, "key-1").getStatus());
        verify(gateway).authorize(any(com.jennparker.auction.domain.Payment.class), eq("key-1"));
    }

    @Test
    void returnsTheExistingPaymentForAnIdempotentRetry() {
        CustomerRepository customers = mock(CustomerRepository.class);
        PaymentMethodRepository methods = mock(PaymentMethodRepository.class);
        PaymentRepository payments = mock(PaymentRepository.class);
        PaymentEntity existing = mock(PaymentEntity.class);
        when(customers.existsById("cust-1")).thenReturn(true);
        PaymentMethodEntity method = mock(PaymentMethodEntity.class);
        when(methods.findByIdAndCustomerId("pm-1", "cust-1")).thenReturn(Optional.of(method));
        when(method.isAuthorized()).thenReturn(true);
        when(payments.findByIdempotencyKeyAndCustomerId("key-1", "cust-1")).thenReturn(Optional.of(existing));
        when(existing.toDomain()).thenReturn(new com.jennparker.auction.domain.Payment("pay-1", "cust-1", "pm-1", BigDecimal.TEN, "AUTHORIZED", java.time.Instant.now()));

        PaymentService service = new PaymentService(customers, methods, payments, mock(PaymentGateway.class));
        assertEquals("pay-1", service.create("cust-1", "pm-1", BigDecimal.TEN, "key-1").getId());
        verify(payments).findByIdempotencyKeyAndCustomerId("key-1", "cust-1");
    }

    @Test
    void recordsProcessorFailureAsFailed() {
        CustomerRepository customers = mock(CustomerRepository.class);
        PaymentMethodRepository methods = mock(PaymentMethodRepository.class);
        PaymentRepository payments = mock(PaymentRepository.class);
        PaymentGateway gateway = mock(PaymentGateway.class);
        PaymentMethodEntity method = mock(PaymentMethodEntity.class);
        PaymentEntity entity = mock(PaymentEntity.class);
        when(customers.existsById("cust-1")).thenReturn(true);
        when(methods.findByIdAndCustomerId("pm-1", "cust-1")).thenReturn(Optional.of(method));
        when(method.isAuthorized()).thenReturn(true);
        when(payments.findByIdempotencyKeyAndCustomerId("key-1", "cust-1")).thenReturn(Optional.empty());
        when(payments.save(any(PaymentEntity.class))).thenReturn(entity);
        when(entity.toDomain()).thenReturn(new com.jennparker.auction.domain.Payment("pay-1", "cust-1", "pm-1", BigDecimal.TEN, "PROCESSING", java.time.Instant.now()));
        doThrow(new RuntimeException("declined")).when(gateway).authorize(any(com.jennparker.auction.domain.Payment.class), eq("key-1"));

        PaymentService service = new PaymentService(customers, methods, payments, gateway);

        assertThrows(PaymentService.PaymentAuthorizationException.class,
                () -> service.create("cust-1", "pm-1", BigDecimal.TEN, "key-1"));
        verify(entity).updateStatus("FAILED");
    }
}
