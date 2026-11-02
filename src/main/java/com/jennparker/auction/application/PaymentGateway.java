package com.jennparker.auction.application;

import com.jennparker.auction.domain.Order;
import com.jennparker.auction.domain.Payment;
import java.math.BigDecimal;

public interface PaymentGateway {
    void authorize(Order order, String idempotencyKey);

    /**
     * Authorizes a specific payment method and amount. The default keeps the
     * original adapter contract source-compatible while real gateways can use
     * the payment method and split amount explicitly.
     */
    default void authorize(Order order, String paymentMethodId, BigDecimal amount, String idempotencyKey) {
        authorize(order, idempotencyKey);
    }

    default void authorize(Payment payment, String idempotencyKey) { }
}
