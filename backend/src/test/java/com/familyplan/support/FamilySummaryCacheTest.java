package com.familyplan.support;

import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class FamilySummaryCacheTest {

    @Test
    void reusesAValueUntilTtlOrFamilyEviction() {
        MutableClock clock = new MutableClock(Instant.parse("2026-11-04T00:00:00Z"));
        FamilySummaryCache cache = new FamilySummaryCache(Duration.ofSeconds(30), clock);
        AtomicInteger loads = new AtomicInteger();

        assertEquals("first", cache.get(7L, "overview:2026-11", () -> {
            loads.incrementAndGet();
            return "first";
        }));
        clock.plusSeconds(29);
        assertEquals("first", cache.get(7L, "overview:2026-11", () -> {
            loads.incrementAndGet();
            return "second";
        }));
        assertEquals(1, loads.get());

        assertEquals("other-name", cache.get(7L, "trend:2026-11", () -> "other-name"));
        assertEquals("other-family", cache.get(8L, "overview:2026-11", () -> "other-family"));

        cache.evictFamily(7L);
        assertEquals("after-evict", cache.get(7L, "overview:2026-11", () -> "after-evict"));
        assertEquals("other-family", cache.get(8L, "overview:2026-11", () -> "should-not-load"));
    }

    @Test
    void expiredEntryLoadsAgainAndFailuresAreNotCached() {
        MutableClock clock = new MutableClock(Instant.parse("2026-11-04T00:00:00Z"));
        FamilySummaryCache cache = new FamilySummaryCache(Duration.ofMillis(50), clock);
        AtomicInteger loads = new AtomicInteger();

        assertThrows(IllegalStateException.class, () -> cache.get(1L, "monthly:2026", () -> {
            loads.incrementAndGet();
            throw new IllegalStateException("db down");
        }));
        assertEquals("ok", cache.get(1L, "monthly:2026", () -> {
            loads.incrementAndGet();
            return "ok";
        }));

        clock.plusMillis(50);
        assertEquals("fresh", cache.get(1L, "monthly:2026", () -> "fresh"));
        assertEquals(2, loads.get());
    }

    @Test
    void rejectsMissingFamilyAndNonPositiveTtl() {
        FamilySummaryCache cache = new FamilySummaryCache(Duration.ofSeconds(1));
        assertThrows(IllegalArgumentException.class, () -> cache.get(null, "overview", () -> "x"));
        assertThrows(IllegalArgumentException.class, () -> new FamilySummaryCache(Duration.ZERO));
        cache.evictFamily(null);
    }

    private static final class MutableClock extends Clock {
        private Instant instant;

        private MutableClock(Instant instant) {
            this.instant = instant;
        }

        private void plusSeconds(long seconds) {
            instant = instant.plusSeconds(seconds);
        }

        private void plusMillis(long millis) {
            instant = instant.plusMillis(millis);
        }

        @Override
        public ZoneId getZone() {
            return ZoneId.of("UTC");
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return instant;
        }
    }
}
