package com.jennparker.auction.application;

import com.jennparker.auction.persistence.OrderEntity;
import com.jennparker.auction.persistence.OrderRepository;
import com.jennparker.auction.persistence.OutboxEventEntity;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** Publishes committed outbox rows and safely retries transient SQS failures. */
@Component
@ConditionalOnProperty(name = "payment.outbox.enabled", havingValue = "true", matchIfMissing = true)
public class OutboxPublisher {
    private final OutboxClaimService claims;
    private final OrderRepository orders;
    private final PaymentEventPublisher publisher;
    private final int batchSize;
    private final long lockSeconds;

    public OutboxPublisher(OutboxClaimService claims, OrderRepository orders, PaymentEventPublisher publisher,
                           @Value("${payment.outbox.batch-size:20}") int batchSize,
                           @Value("${payment.outbox.lock-seconds:60}") long lockSeconds) {
        this.claims = claims; this.orders = orders; this.publisher = publisher;
        this.batchSize = batchSize; this.lockSeconds = lockSeconds;
    }

    @Scheduled(fixedDelayString = "${payment.outbox.poll-delay-ms:1000}")
    public void publishDueEvents() {
        for (OutboxEventEntity event : claims.claimBatch(batchSize, lockSeconds)) {
            try {
                OrderEntity order = orders.findById(event.getOrderId()).orElseThrow(IllegalStateException::new);
                publisher.publish(order.toDomain());
                claims.markPublished(event);
            } catch (RuntimeException ex) {
                claims.markFailed(event, ex);
            }
        }
    }
}
