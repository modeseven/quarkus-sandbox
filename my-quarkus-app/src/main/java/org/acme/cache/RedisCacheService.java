package org.acme.cache;

import io.quarkus.redis.datasource.RedisDataSource;
import io.quarkus.redis.datasource.value.ValueCommands;
import io.quarkus.redis.datasource.keys.KeyCommands;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.acme.cache.qualifiers.RedisCacheImpl;
import org.jboss.logging.Logger;

/**
 * Redis cache implementation using Quarkus Redis client
 */
@ApplicationScoped
@RedisCacheImpl
public class RedisCacheService implements CacheService {

    private static final Logger LOG = Logger.getLogger(RedisCacheService.class);

    @Inject
    RedisDataSource redisDataSource;

    @PostConstruct
    void init() {
        try {
            // Perform a lightweight command to establish the connection eagerly.
            // Using EXISTS on a random key triggers a connection and is safe across Redis/Valkey.
            getKeyCommands().exists("__startup_ping__");
            LOG.info("Redis cache connection initialized successfully");
        } catch (Exception e) {
            LOG.errorf("Failed to initialize Redis cache connection: %s", e.getMessage());
        }
    }

    private ValueCommands<String, String> getValueCommands() {
        return redisDataSource.value(String.class);
    }

    private KeyCommands<String> getKeyCommands() {
        return redisDataSource.key(String.class);
    }

    @Override
    public String get(String key) {
        if (key == null) {
            return null;
        }
        try {
            return getValueCommands().get(key);
        } catch (Exception e) {
            LOG.errorf("Error retrieving key '%s' from Redis cache: %s", key, e.getMessage());
            return null;
        }
    }

    @Override
    public void put(String key, String value, int ttlSeconds) {
        if (key == null || value == null) {
            return;
        }
        try {
            ValueCommands<String, String> commands = getValueCommands();
            if (ttlSeconds > 0) {
                commands.setex(key, ttlSeconds, value);
            } else {
                commands.set(key, value);
            }
        } catch (Exception e) {
            LOG.errorf("Error storing key '%s' in Redis cache: %s", key, e.getMessage());
        }
    }

    @Override
    public void clear(String key) {
        if (key == null) {
            return;
        }
        try {
            getKeyCommands().del(key);
        } catch (Exception e) {
            LOG.errorf("Error clearing key '%s' from Redis cache: %s", key, e.getMessage());
        }
    }

    @Override
    public void clearAll() {
        try {
            // For now, we'll implement a simple approach
            // In a real implementation, you might want to use a different strategy
            LOG.warn("clearAll() not fully implemented for Redis - consider using a different approach");
        } catch (Exception e) {
            LOG.errorf("Error clearing all data from Redis cache: %s", e.getMessage());
        }
    }
}
