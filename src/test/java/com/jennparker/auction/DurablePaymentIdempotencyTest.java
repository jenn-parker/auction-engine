package com.jennparker.auction;

import com.jennparker.auction.application.PaymentGateway;
import com.jennparker.auction.application.PaymentOrchestrator;
import com.jennparker.auction.domain.Order;
import com.jennparker.auction.persistence.PaymentAttemptEntity;
import com.jennparker.auction.persistence.PaymentAttemptRepository;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class DurablePaymentIdempotencyTest {
    @Test
    void claimsTheDatabaseKeyBeforeCallingTheGateway() {
        PaymentAttemptRepository attempts = mock(PaymentAttemptRepository.class);
        when(attempts.tryClaim(any(), any(), eq("lot-1:cust-1"))).thenReturn(1);
        PaymentGateway gateway = mock(PaymentGateway.class);
        PaymentOrchestrator orchestrator = new PaymentOrchestrator(gateway, null, attempts);
        Order order = new Order("lot-1", "cust-1", BigDecimal.TEN, Instant.EPOCH);

        orchestrator.authorize(order);

        verify(gateway).authorize(order, "lot-1:cust-1");
        verify(attempts).markSettled("lot-1:cust-1");
        assertEquals(Order.Status.SETTLED, order.getStatus());
    }

    @Test
    void doesNotCallTheGatewayWhenAnotherWorkerOwnsTheKey() {
        PaymentAttemptRepository attempts = mock(PaymentAttemptRepository.class);
        when(attempts.tryClaim(any(), any(), eq("lot-1:cust-1"))).thenReturn(0);
        when(attempts.findByIdempotencyKey("lot-1:cust-1")).thenReturn(mock(PaymentAttemptEntity.class));
        PaymentGateway gateway = mock(PaymentGateway.class);
        PaymentOrchestrator orchestrator = new PaymentOrchestrator(gateway, null, attempts);
        Order order = new Order("lot-1", "cust-1", BigDecimal.TEN, Instant.EPOCH);

        orchestrator.authorize(order);

        verifyNoInteractions(gateway);
        verify(attempts, never()).markSettled(anyString());
        assertEquals(Order.Status.PENDING, order.getStatus());
    }
}
