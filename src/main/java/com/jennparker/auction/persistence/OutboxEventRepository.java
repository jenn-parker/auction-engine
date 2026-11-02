package com.jennparker.auction.persistence;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface OutboxEventRepository extends JpaRepository<OutboxEventEntity, UUID> {
    @Query(value = "SELECT * FROM outbox_events WHERE available_at <= :now AND (status = 'PENDING' OR (status = 'PROCESSING' AND locked_until < :now)) ORDER BY created_at FOR UPDATE SKIP LOCKED", nativeQuery = true)
    List<OutboxEventEntity> findDueForUpdate(Instant now, Pageable pageable);
}
