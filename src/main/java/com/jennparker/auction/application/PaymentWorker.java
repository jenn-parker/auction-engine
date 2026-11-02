package com.jennparker.auction.application;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jennparker.auction.domain.Order;
import com.jennparker.auction.persistence.OrderEntity;
import com.jennparker.auction.persistence.OrderRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.DeleteMessageRequest;
import software.amazon.awssdk.services.sqs.model.ReceiveMessageRequest;
import software.amazon.awssdk.services.sqs.model.Message;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

/** Consumes payment authorization messages with SQS at-least-once semantics. */
@Component
@ConditionalOnProperty(name = "payment.worker.enabled", havingValue = "true", matchIfMissing = true)
public class PaymentWorker {
    private final SqsClient sqs;
    private final ObjectMapper objectMapper;
    private final OrderRepository orders;
    private final PaymentOrchestrator orchestrator;
    private final String queueUrl;

    public PaymentWorker(SqsClient sqs, ObjectMapper objectMapper, OrderRepository orders, PaymentOrchestrator orchestrator,
                         @Value("${PAYMENT_QUEUE_URL:http://localhost:4566/000000000000/auction-payment-events}") String queueUrl) {
        this.sqs = Objects.requireNonNull(sqs, "sqs");
        this.objectMapper = Objects.requireNonNull(objectMapper, "objectMapper");
        this.orders = Objects.requireNonNull(orders, "orders");
        this.orchestrator = Objects.requireNonNull(orchestrator, "orchestrator");
        this.queueUrl = Objects.requireNonNull(queueUrl, "queueUrl");
    }

    @Scheduled(fixedDelayString = "${payment.worker.poll-delay-ms:1000}")
    public void poll() {
        List<Message> messages = sqs.receiveMessage(ReceiveMessageRequest.builder().queueUrl(queueUrl).maxNumberOfMessages(10).waitTimeSeconds(1).build()).messages();
        if (messages == null) return;
        for (Message message : messages) if (message != null) process(message);
    }

    void process(Message message) {
        try {
            if (message.body() == null) throw new IllegalArgumentException("Payment event body is required");
            JsonNode event = objectMapper.readTree(message.body());
            JsonNode orderIdNode = event == null ? null : event.get("orderId");
            if (orderIdNode == null || orderIdNode.isNull() || !orderIdNode.isTextual()) {
                throw new IllegalArgumentException("Payment event orderId is required");
            }
            UUID orderId = UUID.fromString(orderIdNode.asText());
            OrderEntity entity = orders.findById(orderId).orElseThrow(IllegalStateException::new);
            Order order = entity.toDomain();
            if (order.getStatus() == Order.Status.PENDING) {
                try { orchestrator.authorize(order); }
                catch (RuntimeException failure) { entity.updateStatus(order.getStatus()); orders.save(entity); throw failure; }
                if (order.getStatus() == Order.Status.PENDING) return;
                entity.updateStatus(order.getStatus());
                orders.save(entity);
            }
            if (order.getStatus() != Order.Status.PENDING) delete(message);
        } catch (Exception failure) {
            // Leave the message invisible until SQS makes it visible again for retry.
        }
    }

    private void delete(Message message) {
        sqs.deleteMessage(DeleteMessageRequest.builder().queueUrl(queueUrl).receiptHandle(message.receiptHandle()).build());
    }
}
