package com.jennparker.auction;

import com.jennparker.auction.application.*;
import com.jennparker.auction.domain.Order;
import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import static org.junit.jupiter.api.Assertions.*;

class PaymentOrchestratorTest {
    @Test void repeatedAuthorizationUsesOneGatewayCall() {
        AtomicInteger calls = new AtomicInteger();
        PaymentGateway gateway = (order, key) -> calls.incrementAndGet();
        PaymentOrchestrator orchestrator = new PaymentOrchestrator(gateway);
        Order order = new Order("lot-1", "cust-1", BigDecimal.TEN, Instant.EPOCH);
        orchestrator.authorize(order);
        assertDoesNotThrow(() -> orchestrator.authorize(order));
        assertEquals(1, calls.get());
        assertEquals(Order.Status.SETTLED, order.getStatus());
    }

    @Test
    void concurrentRetriesStillAuthorizeOnce() throws Exception {
        AtomicInteger calls = new AtomicInteger();
        PaymentGateway gateway = (order, key) -> {
            calls.incrementAndGet();
            try {
                Thread.sleep(10);
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
                throw new RuntimeException(ex);
            }
        };
        PaymentOrchestrator orchestrator = new PaymentOrchestrator(gateway);
        Order order = new Order("lot-1", "cust-1", BigDecimal.TEN, Instant.EPOCH);
        ExecutorService pool = Executors.newFixedThreadPool(16);
        CountDownLatch start = new CountDownLatch(1);

        for (int i = 0; i < 50; i++) {
            pool.submit(() -> {
                start.await();
                orchestrator.authorize(order);
                return null;
            });
        }
        start.countDown();
        pool.shutdown();
        assertTrue(pool.awaitTermination(5, TimeUnit.SECONDS));
        assertEquals(1, calls.get());
        assertEquals(Order.Status.SETTLED, order.getStatus());
    }
}
