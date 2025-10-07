package org.acme.cache;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.acme.cache.qualifiers.DefaultCacheImpl;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * In-memory cache implementation for development and testing.
 * This is a basic, non-production-ready, thread-safe implementation.
 */
@ApplicationScoped
@DefaultCacheImpl
public class InMemoryCacheService implements CacheService {

    private static final long NO_EXPIRY = Long.MAX_VALUE;

    private static final class CacheEntry {
        final String value;
        final long expiresAtEpochMs;

        CacheEntry(String value, long expiresAtEpochMs) {
            this.value = value;
            this.expiresAtEpochMs = expiresAtEpochMs;
        }

        boolean isExpired(long nowMs) {
            return expiresAtEpochMs <= nowMs;
        }
    }

    private final ConcurrentHashMap<String, CacheEntry> cache = new ConcurrentHashMap<>();
    private final ScheduledExecutorService cleanupExecutor = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread t = new Thread(r, "in-memory-cache-cleaner");
        t.setDaemon(true);
        return t;
    });

    @PostConstruct
    void initCleanupTask() {
        // Initial delay 3 minutes, then every 3 minutes
        cleanupExecutor.scheduleAtFixedRate(this::purgeExpiredEntries, 3, 3, TimeUnit.MINUTES);
    }

    @PreDestroy
    void shutdownCleanupTask() {
        cleanupExecutor.shutdownNow();
    }

    @Override
    public String get(String key) {
        if (key == null) {
            return null;
        }
        CacheEntry entry = cache.get(key);
        if (entry == null) {
            return null;
        }
        long now = System.currentTimeMillis();
        if (entry.isExpired(now)) {
            // Lazy eviction on read
            cache.remove(key, entry);
            return null;
        }
        return entry.value;
    }

    @Override
    public void put(String key, String value, int ttlSeconds) {
        if (key == null || value == null) {
            return;
        }
        long expiresAt = ttlSeconds > 0
                ? System.currentTimeMillis() + TimeUnit.SECONDS.toMillis(ttlSeconds)
                : NO_EXPIRY;
        cache.put(key, new CacheEntry(value, expiresAt));
    }

    @Override
    public void clear(String key) {
        if (key != null) {
            cache.remove(key);
        }
    }

    @Override
    public void clearAll() {
        cache.clear();
    }

    private void purgeExpiredEntries() {
        long now = System.currentTimeMillis();
        cache.entrySet().removeIf(e -> e.getValue() != null && e.getValue().isExpired(now));
    }
}
