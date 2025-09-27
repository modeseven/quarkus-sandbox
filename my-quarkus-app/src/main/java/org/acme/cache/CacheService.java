package org.acme.cache;

/**
 * Interface for cache operations that can be implemented by different cache providers
 * (Redis, In-Memory, etc.)
 */
public interface CacheService {

    /**
     * Attempts to retrieve a value associated with the given key.
     * @param key The cache key.
     * @return The cached value (String), or null if the key is not found.
     */
    String get(String key);

    /**
     * Stores a key-value pair in the cache.
     * @param key The cache key.
     * @param value The serialized value (String) to store.
     * @param ttlSeconds The time-to-live in seconds. If 0 or negative, TTL is indefinite.
     */
    void put(String key, String value, int ttlSeconds);

    /**
     * Invalidates (removes) a specific key from the cache.
     * @param key The key to remove.
     */
    void clear(String key);

    /**
     * Flushes all data from the cache.
     */
    void clearAll();
}
