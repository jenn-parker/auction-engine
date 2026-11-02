package com.jennparker.auction;

import com.jennparker.auction.domain.*;
import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import java.time.*;
import java.util.concurrent.*;
import static org.junit.jupiter.api.Assertions.*;

class LotConcurrencyTest {
    @Test void onlyOneBidderWinsEachContestedAmount() throws Exception {
        Lot lot = new Lot("lot-1", Instant.parse("2026-11-02T00:00:00Z"));
        ExecutorService pool = Executors.newFixedThreadPool(8); CountDownLatch start = new CountDownLatch(1);
        CompletionService<Boolean> results = new ExecutorCompletionService<Boolean>(pool);
        for (int i = 1; i <= 100; i++) { final int amount = i; results.submit(() -> { start.await(); return lot.accept(new Bid("c" + amount, BigDecimal.valueOf(amount), Instant.parse("2026-11-01T20:00:00Z")), Clock.systemUTC()); }); }
        start.countDown(); int accepted = 0; for (int i = 0; i < 100; i++) if (results.take().get()) accepted++;
        pool.shutdownNow(); assertTrue(accepted >= 1); assertEquals(BigDecimal.valueOf(100), lot.getHighestBid().getAmount());
    }
}
