package com.jennparker.auction.config;

import com.jennparker.auction.application.AuctionService;
import com.jennparker.auction.application.AuctionMetrics;
import com.jennparker.auction.domain.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import io.micrometer.core.instrument.MeterRegistry;
import java.time.Clock;

@Configuration
public class AuctionConfiguration {
    @Bean public Clock clock() { return Clock.systemUTC(); }
    @Bean public AuctionMetrics auctionMetrics(MeterRegistry registry) { return new AuctionMetrics(registry); }
    @Bean public AuctionService auctionService(CustomerService customers, Clock clock, com.jennparker.auction.application.AuctionStateProvider auctionStates, com.jennparker.auction.persistence.LotRepository lots, com.jennparker.auction.persistence.BidRepository bids, com.jennparker.auction.persistence.OrderRepository orders, com.jennparker.auction.persistence.OutboxEventRepository outbox, AuctionMetrics metrics, com.jennparker.auction.application.CustomerCreditService credits, com.jennparker.auction.persistence.PaymentAllocationRepository allocations, com.jennparker.auction.application.PaymentOrchestrator payments, com.jennparker.auction.persistence.PaymentRepository paymentRepository) { return new AuctionService(customers, clock, auctionStates, lots, bids, orders, outbox, metrics, credits, allocations, payments, paymentRepository); }
}
