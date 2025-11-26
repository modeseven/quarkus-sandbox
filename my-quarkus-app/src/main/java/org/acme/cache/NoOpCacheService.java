package org.acme.cache;

/**
 * No-op cache service implementation that does nothing.
 * Used when caching is disabled - the feature "lays dormant".
 * This is a simple POJO, not a CDI bean, created manually by the producer.
 */
public class NoOpCacheService implements CacheService {

    @Override
    public String get(String key) {
        // No-op: feature is dormant
        return null;
    }

    @Override
    public void put(String key, String value, int ttlSeconds) {
        // No-op: feature is dormant
    }

    @Override
    public void clear(String key) {
        // No-op: feature is dormant
    }

    @Override
    public void clearAll() {
        // No-op: feature is dormant
    }
}

