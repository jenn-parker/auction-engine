package com.jennparker.auction.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jennparker.auction.application.PaymentEventPublisher;
import com.jennparker.auction.infrastructure.aws.SqsPaymentEventPublisher;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.SqsClientBuilder;

import java.net.URI;

@Configuration
public class AwsConfiguration {
    @Bean(destroyMethod = "close")
    public SqsClient sqsClient(
            @Value("${AWS_ENDPOINT:}") String endpoint,
            @Value("${AWS_REGION:us-east-1}") String region,
            @Value("${AWS_ACCESS_KEY_ID:test}") String accessKey,
            @Value("${AWS_SECRET_ACCESS_KEY:test}") String secretKey) {
        SqsClientBuilder builder = SqsClient.builder()
                .region(Region.of(region))
                .credentialsProvider(StaticCredentialsProvider.create(AwsBasicCredentials.create(accessKey, secretKey)));
        if (endpoint != null && !endpoint.trim().isEmpty()) builder.endpointOverride(java.util.Objects.requireNonNull(URI.create(endpoint)));
        return builder.build();
    }

    @Bean
    public PaymentEventPublisher paymentEventPublisher(
            SqsClient sqsClient, ObjectMapper objectMapper,
            @Value("${PAYMENT_QUEUE_URL:http://localhost:4566/000000000000/auction-payment-events}") String queueUrl) {
        return new SqsPaymentEventPublisher(sqsClient, objectMapper, queueUrl);
    }
}
