package com.jennparker.auction.application;

import com.jennparker.auction.domain.Order;

public interface PaymentEventPublisher {
    void publish(Order order);
}
