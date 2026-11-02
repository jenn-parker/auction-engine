package com.jennparker.auction;

import com.jennparker.auction.application.CustomerManagementService;
import com.jennparker.auction.application.PaymentQueryService;
import com.jennparker.auction.domain.Payment;
import com.jennparker.auction.persistence.CustomerRepository;
import com.jennparker.auction.persistence.PaymentEntity;
import com.jennparker.auction.persistence.PaymentRepository;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class PaymentQueryServiceTest {
    @Test
    void listsOnlyTheCustomersPayments() {
        CustomerRepository customers = mock(CustomerRepository.class);
        PaymentRepository payments = mock(PaymentRepository.class);
        PaymentEntity entity = mock(PaymentEntity.class);
        when(customers.existsById("cust-1")).thenReturn(true);
        when(payments.findAllByCustomerIdOrderByCreatedAtDesc("cust-1")).thenReturn(Collections.singletonList(entity));
        when(entity.toDomain()).thenReturn(new Payment("pay-1", "cust-1", "pm-1", BigDecimal.TEN, "APPLIED", Instant.now()));

        PaymentQueryService service = new PaymentQueryService(customers, payments);

        assertEquals("pay-1", service.list("cust-1").get(0).getId());
        verify(payments).findAllByCustomerIdOrderByCreatedAtDesc("cust-1");
    }

    @Test
    void rejectsPaymentsForAnUnknownCustomer() {
        CustomerRepository customers = mock(CustomerRepository.class);
        when(customers.existsById("missing")).thenReturn(false);
        PaymentQueryService service = new PaymentQueryService(customers, mock(PaymentRepository.class));

        assertThrows(CustomerManagementService.CustomerNotFoundException.class, () -> service.list("missing"));
    }
}
