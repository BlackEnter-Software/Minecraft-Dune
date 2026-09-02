package com.blackenter.minecraftdune.worldgen.arrakis;

import com.blackenter.minecraftdune.MinecraftDune;
import net.minecraft.world.level.ChunkPos;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.LongAdder;

/**
 * Opt-in, rate-limited diagnostics for the analytical Arrakis terrain pipeline.
 *
 * <p>Enable with {@code -Dminecraftdune.terrainMetrics=true}. The diagnostics only observe
 * evaluation work and never participate in terrain decisions.</p>
 */
final class TerrainGenerationMetrics {
    private static final boolean ENABLED = Boolean.getBoolean("minecraftdune.terrainMetrics");
    private static final long SLOW_CHUNK_NANOS = TimeUnit.MILLISECONDS.toNanos(
            Long.getLong("minecraftdune.slowChunkMillis", 50L)
    );
    private static final long SLOW_LOG_INTERVAL_NANOS = TimeUnit.SECONDS.toNanos(10L);
    private static final long SUMMARY_INTERVAL = 256L;

    private static final LongAdder CHUNKS = new LongAdder();
    private static final LongAdder QUERIES = new LongAdder();
    private static final LongAdder COLUMN_EVALUATIONS = new LongAdder();
    private static final LongAdder CACHE_HITS = new LongAdder();
    private static final LongAdder CHUNK_COLUMN_EVALUATIONS = new LongAdder();
    private static final LongAdder CACHE_BYPASSES = new LongAdder();
    private static final LongAdder FULL_CHUNK_CACHES = new LongAdder();
    private static final AtomicLong WORST_CHUNK_NANOS = new AtomicLong();
    private static final AtomicLong NEXT_SLOW_LOG_NANOS = new AtomicLong();

    private TerrainGenerationMetrics() {
    }

    static Evaluation evaluation() {
        return ENABLED ? new Evaluation() : Evaluation.DISABLED;
    }

    static void recordChunk(ChunkPos chunk, long elapsedNanos, Evaluation evaluation, int cacheSize) {
        if (!ENABLED) {
            return;
        }

        CHUNKS.increment();
        CHUNK_COLUMN_EVALUATIONS.add(evaluation.cacheMisses);
        WORST_CHUNK_NANOS.accumulateAndGet(elapsedNanos, Math::max);
        if (cacheSize == ArrakisTerrainEvaluator.CHUNK_CACHE_LIMIT) {
            FULL_CHUNK_CACHES.increment();
        }
        add(evaluation);
        if (elapsedNanos >= SLOW_CHUNK_NANOS && reserveSlowLog()) {
            MinecraftDune.LOGGER.warn(
                    "Slow Arrakis terrain chunk {} took {} ms ({} column evaluations, {} cache hits, {} cached columns)",
                    chunk,
                    TimeUnit.NANOSECONDS.toMillis(elapsedNanos),
                    evaluation.cacheMisses,
                    evaluation.cacheHits,
                    cacheSize
            );
        }
        logSummaryIfDue();
    }

    static void recordQuery(String query, long elapsedNanos, Evaluation evaluation, int cacheSize) {
        if (!ENABLED) {
            return;
        }

        QUERIES.increment();
        add(evaluation);
        if (elapsedNanos >= SLOW_CHUNK_NANOS && reserveSlowLog()) {
            MinecraftDune.LOGGER.warn(
                    "Slow Arrakis {} query took {} ms ({} column evaluations, {} cache hits, {} cached columns)",
                    query,
                    TimeUnit.NANOSECONDS.toMillis(elapsedNanos),
                    evaluation.cacheMisses,
                    evaluation.cacheHits,
                    cacheSize
            );
        }
        logSummaryIfDue();
    }

    private static void add(Evaluation evaluation) {
        COLUMN_EVALUATIONS.add(evaluation.cacheMisses);
        CACHE_HITS.add(evaluation.cacheHits);
        CACHE_BYPASSES.add(evaluation.cacheBypasses);
    }

    private static boolean reserveSlowLog() {
        long now = System.nanoTime();
        long next = NEXT_SLOW_LOG_NANOS.get();
        return now >= next
                && NEXT_SLOW_LOG_NANOS.compareAndSet(next, now + SLOW_LOG_INTERVAL_NANOS);
    }

    private static void logSummaryIfDue() {
        long operations = CHUNKS.sum() + QUERIES.sum();
        if (operations % SUMMARY_INTERVAL == 0L) {
            MinecraftDune.LOGGER.info(
                    "Arrakis terrain metrics: {} chunks, {} external queries, {} column evaluations, {} cache hits; "
                            + "{} evaluations/chunk, {}% hit rate, {} full chunk caches, {} uncached evaluations, worst {} ms",
                    CHUNKS.sum(),
                    QUERIES.sum(),
                    COLUMN_EVALUATIONS.sum(),
                    CACHE_HITS.sum(),
                    CHUNK_COLUMN_EVALUATIONS.sum() / Math.max(1L, CHUNKS.sum()),
                    100L * CACHE_HITS.sum() / Math.max(1L, CACHE_HITS.sum() + COLUMN_EVALUATIONS.sum()),
                    FULL_CHUNK_CACHES.sum(),
                    CACHE_BYPASSES.sum(),
                    TimeUnit.NANOSECONDS.toMillis(WORST_CHUNK_NANOS.get())
            );
        }
    }

    static final class Evaluation {
        private static final Evaluation DISABLED = new Evaluation(false);

        private final boolean enabled;
        private int cacheHits;
        private int cacheMisses;
        private int cacheBypasses;

        private Evaluation() {
            this(true);
        }

        private Evaluation(boolean enabled) {
            this.enabled = enabled;
        }

        void cacheHit() {
            if (enabled) {
                cacheHits++;
            }
        }

        void cacheMiss() {
            if (enabled) {
                cacheMisses++;
            }
        }

        void cacheBypass() {
            if (enabled) cacheBypasses++;
        }

        int hits() { return cacheHits; }
        int misses() { return cacheMisses; }
        int bypasses() { return cacheBypasses; }
    }
}
