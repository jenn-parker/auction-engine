package com.jennparker.auction.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PaymentRepository extends JpaRepository<PaymentEntity, UUID> {
    List<PaymentEntity> findAllByCustomerIdOrderByCreatedAtDesc(String customerId);
    Optional<PaymentEntity> findByIdAndCustomerId(UUID id, String customerId);
    Optional<PaymentEntity> findByIdempotencyKeyAndCustomerId(String idempotencyKey, String customerId);
}
