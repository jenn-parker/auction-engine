package com.jennparker.auction.config;

import com.jennparker.auction.application.AuctionMetrics;
import com.jennparker.auction.application.PaymentGateway;
import com.jennparker.auction.application.PaymentOrchestrator;
import com.jennparker.auction.persistence.PaymentAttemptRepository;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Local payment adapter; replace this bean with the real gateway integration in deployment. */
@Configuration
public class PaymentWorkerConfiguration {
    @Bean
    @ConditionalOnProperty(name = "payment.gateway.mock", havingValue = "true", matchIfMissing = true)
    public PaymentGateway mockPaymentGateway() {
        return (order, idempotencyKey) -> { };
    }

    @Bean
    public PaymentOrchestrator paymentOrchestrator(PaymentGateway gateway, AuctionMetrics metrics, PaymentAttemptRepository attempts) {
        return new PaymentOrchestrator(gateway, metrics, attempts);
    }
}
