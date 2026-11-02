package com.jennparker.auction.infrastructure.aws;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jennparker.auction.application.PaymentEventPublisher;
import com.jennparker.auction.domain.Order;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.SendMessageRequest;

import java.util.HashMap;
import java.util.Map;

public final class SqsPaymentEventPublisher implements PaymentEventPublisher {
    private final SqsClient client;
    private final ObjectMapper objectMapper;
    private final String queueUrl;

    public SqsPaymentEventPublisher(SqsClient client, ObjectMapper objectMapper, String queueUrl) {
        this.client = client; this.objectMapper = objectMapper; this.queueUrl = queueUrl;
    }

    @Override
    public void publish(Order order) {
        Map<String, Object> event = new HashMap<String, Object>();
        event.put("eventType", "PaymentAuthorizationRequested");
        event.put("orderId", order.getId());
        event.put("lotId", order.getLotId());
        event.put("customerId", order.getCustomerId());
        event.put("amount", order.getAmount());
        try {
            client.sendMessage(SendMessageRequest.builder().queueUrl(queueUrl)
                    .messageBody(objectMapper.writeValueAsString(event)).build());
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Unable to serialize payment event", ex);
        }
    }
}
