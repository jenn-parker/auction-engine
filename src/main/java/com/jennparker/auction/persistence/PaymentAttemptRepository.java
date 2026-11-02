package com.jennparker.auction.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

public interface PaymentAttemptRepository extends JpaRepository<PaymentAttemptEntity, UUID> {
    @Modifying
    @Transactional
    @Query(value = "INSERT INTO payment_attempts (id, order_id, idempotency_key, status, created_at) VALUES (:id, :orderId, :idempotencyKey, 'PROCESSING', CURRENT_TIMESTAMP) ON CONFLICT (idempotency_key) DO NOTHING", nativeQuery = true)
    int tryClaim(@Param("id") UUID id, @Param("orderId") UUID orderId, @Param("idempotencyKey") String idempotencyKey);

    @Modifying
    @Transactional
    @Query("UPDATE PaymentAttemptEntity p SET p.status = 'SETTLED' WHERE p.idempotencyKey = :idempotencyKey")
    int markSettled(@Param("idempotencyKey") String idempotencyKey);

    @Modifying
    @Transactional
    @Query("UPDATE PaymentAttemptEntity p SET p.status = 'FAILED' WHERE p.idempotencyKey = :idempotencyKey")
    int markFailed(@Param("idempotencyKey") String idempotencyKey);

    PaymentAttemptEntity findByIdempotencyKey(String idempotencyKey);
}
