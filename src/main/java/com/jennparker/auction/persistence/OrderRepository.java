package com.jennparker.auction.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;
import java.util.List;

public interface OrderRepository extends JpaRepository<OrderEntity, UUID> {
    List<OrderEntity> findByCustomerIdAndStatusNotOrderByCreatedAtAsc(String customerId, String settledStatus);
    List<OrderEntity> findByCustomerIdOrderByCreatedAtAsc(String customerId);
}
