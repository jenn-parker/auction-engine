package com.jennparker.auction;

import com.jennparker.auction.domain.Bid;
import com.jennparker.auction.domain.Lot;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Hot-path throughput harness. It is not a unit test and is excluded from
 * Surefire by its name; run it with: mvn -Pperformance test exec:java.
 */
public final class BidIngestionBenchmark {
    public static void main(String[] args) throws Exception {
        final int workers = 8;
        final int operations = 100_000;
        final Lot lot = new Lot("benchmark-lot", Instant.now().plusSeconds(3600));
        final AtomicLong amount = new AtomicLong();
        final AtomicLong accepted = new AtomicLong();
        ExecutorService pool = Executors.newFixedThreadPool(workers);
        CountDownLatch ready = new CountDownLatch(workers);
        CountDownLatch start = new CountDownLatch(1);
        int perWorker = operations / workers;

        for (int worker = 0; worker < workers; worker++) {
            pool.submit(() -> {
                ready.countDown();
                start.await();
                for (int i = 0; i < perWorker; i++) {
                    long nextAmount = amount.incrementAndGet();
                    if (lot.accept(new Bid("benchmark-customer", BigDecimal.valueOf(nextAmount), Instant.now()), Clock.systemUTC())) {
                        accepted.incrementAndGet();
                    }
                }
                return null;
            });
        }

        ready.await();
        long started = System.nanoTime();
        start.countDown();
        pool.shutdown();
        if (!pool.awaitTermination(60, TimeUnit.SECONDS)) throw new IllegalStateException("Benchmark did not finish");
        double seconds = (System.nanoTime() - started) / 1_000_000_000.0;
        System.out.printf("bid_acceptance workers=%d operations=%d accepted=%d throughput_ops_per_second=%.2f%n",
                workers, operations, accepted.get(), operations / seconds);
    }
}
