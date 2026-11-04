package com.familyplan.support;

import java.time.Clock;
import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

/**
 * 按家庭缓存仪表盘和月报结果。写入收支后由拦截器整户失效。
 * 过期或失效之前，同一个键只计算一次；计算失败不写入缓存。
 */
public final class FamilySummaryCache {

    private final long ttlMillis;
    private final Clock clock;
    private final ConcurrentHashMap<String, Entry> entries = new ConcurrentHashMap<>();

    public FamilySummaryCache(Duration ttl) {
        this(ttl, Clock.systemUTC());
    }

    public FamilySummaryCache(Duration ttl, Clock clock) {
        if (ttl == null || ttl.isZero() || ttl.isNegative()) {
            throw new IllegalArgumentException("summary cache ttl must be positive");
        }
        if (clock == null) {
            throw new IllegalArgumentException("clock is required");
        }
        this.ttlMillis = ttl.toMillis();
        this.clock = clock;
    }

    public <T> T get(Long familyId, String name, Supplier<T> loader) {
        if (familyId == null) {
            throw new IllegalArgumentException("familyId is required");
        }
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("cache name is required");
        }
        if (loader == null) {
            throw new IllegalArgumentException("loader is required");
        }
        String key = familyId + "\u0000" + name;
        long now = clock.millis();
        Entry current = entries.get(key);
        if (current != null && current.expiresAtMillis > now) {
            @SuppressWarnings("unchecked")
            T cached = (T) current.value;
            return cached;
        }
        T loaded = loader.get();
        entries.put(key, new Entry(loaded, now + ttlMillis));
        return loaded;
    }

    public void evictFamily(Long familyId) {
        if (familyId == null) {
            return;
        }
        String prefix = familyId + "\u0000";
        entries.keySet().removeIf(key -> key.startsWith(prefix));
    }

    public int size() {
        return entries.size();
    }

    private static final class Entry {
        private final Object value;
        private final long expiresAtMillis;

        private Entry(Object value, long expiresAtMillis) {
            this.value = value;
            this.expiresAtMillis = expiresAtMillis;
        }
    }
}
