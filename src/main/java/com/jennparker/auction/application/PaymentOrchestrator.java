package com.jennparker.auction.application;

import com.jennparker.auction.domain.Order;
import com.jennparker.auction.persistence.PaymentAttemptEntity;
import com.jennparker.auction.persistence.PaymentAttemptRepository;
import java.math.BigDecimal;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class PaymentOrchestrator {
    private final PaymentGateway gateway;
    private final Set<String> processedKeys = ConcurrentHashMap.newKeySet();
    private final AuctionMetrics metrics;
    private final PaymentAttemptRepository attempts;
    public PaymentOrchestrator(PaymentGateway gateway) { this(gateway, AuctionMetrics.noop()); }
    public PaymentOrchestrator(PaymentGateway gateway, AuctionMetrics metrics) { this(gateway, metrics, null); }
    public PaymentOrchestrator(PaymentGateway gateway, AuctionMetrics metrics, PaymentAttemptRepository attempts) {
        this.gateway = gateway; this.metrics = metrics == null ? AuctionMetrics.noop() : metrics; this.attempts = attempts;
    }
    public void authorize(Order order) {
        long started = System.nanoTime();
        String key = order.getLotId() + ":" + order.getCustomerId();
        if (!claim(key, order)) {
            metrics.recordPayment("duplicate", true, System.nanoTime() - started);
            return;
        }
        order.transitionTo(Order.Status.PROCESSING);
        try {
            gateway.authorize(order, key); order.transitionTo(Order.Status.SETTLED);
            if (attempts != null) attempts.markSettled(key);
            metrics.recordPayment("settled", false, System.nanoTime() - started);
        } catch (RuntimeException ex) {
            order.transitionTo(Order.Status.FAILED);
            if (attempts != null) attempts.markFailed(key);
            metrics.recordPayment("failed", false, System.nanoTime() - started);
            throw ex;
        }
    }

    /** Authorizes a settlement payment without changing the order lifecycle. */
    public void authorizePayment(Order order, String paymentMethodId, BigDecimal amount, String idempotencyKey) {
        long started = System.nanoTime();
        if (!claim(idempotencyKey, order)) {
            metrics.recordPayment("duplicate", true, System.nanoTime() - started);
            return;
        }
        try {
            gateway.authorize(order, paymentMethodId, amount, idempotencyKey);
            if (attempts != null) attempts.markSettled(idempotencyKey);
            metrics.recordPayment("authorized", false, System.nanoTime() - started);
        } catch (RuntimeException ex) {
            if (attempts != null) attempts.markFailed(idempotencyKey);
            metrics.recordPayment("failed", false, System.nanoTime() - started);
            throw ex;
        }
    }

    private boolean claim(String key, Order order) {
        if (attempts == null) return processedKeys.add(key);
        int inserted = attempts.tryClaim(UUID.randomUUID(), UUID.fromString(order.getId()), key);
        if (inserted == 1) return true;
        PaymentAttemptEntity existing = attempts.findByIdempotencyKey(key);
        if (existing == null) throw new IllegalStateException("Payment attempt claim was not persisted");
        return false;
    }
}
