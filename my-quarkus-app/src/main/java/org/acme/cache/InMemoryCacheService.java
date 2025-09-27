package org.acme.cache;

import jakarta.enterprise.context.ApplicationScoped;
import org.acme.cache.qualifiers.DefaultCacheImpl;

import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory cache implementation for development and testing.
 * This is a basic, non-production-ready, thread-safe implementation.
 */
@ApplicationScoped
@DefaultCacheImpl
public class InMemoryCacheService implements CacheService {

    private final ConcurrentHashMap<String, String> cache = new ConcurrentHashMap<>();

    @Override
    public String get(String key) {
        if (key == null) {
            return null;
        }
        return cache.get(key);
    }

    @Override
    public void put(String key, String value, int ttlSeconds) {
        if (key == null || value == null) {
            return;
        }
        // TTL is ignored for this implementation (treated as indefinite)
        cache.put(key, value);
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
}
