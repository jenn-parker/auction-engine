package com.jennparker.auction;

import com.jennparker.auction.domain.Order;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

class OrderLifecycleTest {
    @Test
    void onlyAllowsPendingProcessingAndTerminalTransitions() {
        Order order = new Order("lot-1", "cust-1", BigDecimal.TEN, Instant.EPOCH);
        assertEquals(Order.Status.PENDING, order.getStatus());

        order.transitionTo(Order.Status.PROCESSING);
        order.transitionTo(Order.Status.SETTLED);

        assertEquals(Order.Status.SETTLED, order.getStatus());
        assertThrows(IllegalStateException.class, () -> order.transitionTo(Order.Status.FAILED));
    }

    @Test
    void supportsAProcessingFailureButDoesNotAllowRecovery() {
        Order order = new Order("lot-1", "cust-1", BigDecimal.TEN, Instant.EPOCH);
        order.transitionTo(Order.Status.PROCESSING);
        order.transitionTo(Order.Status.FAILED);

        assertEquals(Order.Status.FAILED, order.getStatus());
        assertThrows(IllegalStateException.class, () -> order.transitionTo(Order.Status.PROCESSING));
    }
}
