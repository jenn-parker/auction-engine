package com.jennparker.auction.application;

import com.jennparker.auction.persistence.OutboxEventEntity;
import com.jennparker.auction.persistence.OutboxEventRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

/** Keeps row-lock transactions separate from network publishing transactions. */
@Service
public class OutboxClaimService {
    private final OutboxEventRepository events;

    public OutboxClaimService(OutboxEventRepository events) { this.events = events; }

    @Transactional
    public List<OutboxEventEntity> claimBatch(int batchSize, long lockSeconds) {
        Instant now = Instant.now();
        List<OutboxEventEntity> due = events.findDueForUpdate(now, PageRequest.of(0, batchSize));
        Instant lockedUntil = now.plusSeconds(lockSeconds);
        for (OutboxEventEntity event : due) event.claim(lockedUntil);
        events.saveAll(due);
        return due;
    }

    @Transactional
    public void markPublished(OutboxEventEntity event) { event.markPublished(Instant.now()); events.save(event); }

    @Transactional
    public void markFailed(OutboxEventEntity event, RuntimeException failure) {
        long backoffSeconds = Math.min(300L, 1L << Math.min(event.getAttempts(), 8));
        event.markFailed(failure.getClass().getSimpleName(), Instant.now().plusSeconds(backoffSeconds));
        events.save(event);
    }
}
