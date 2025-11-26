package org.acme.cache;

import io.quarkus.redis.datasource.RedisDataSource;
import io.quarkus.redis.datasource.keys.KeyCommands;
import io.quarkus.runtime.StartupEvent;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import org.acme.config.CachingConfiguration;
import org.eclipse.microprofile.config.Config;
import org.jboss.logging.Logger;

import java.util.Optional;

/**
 * Bootstrap observer that validates cache configuration and checks Redis health at startup.
 * If Redis is configured but unhealthy, falls back to in-memory cache.
 */
@ApplicationScoped
public class CacheBootstrap {

    private static final Logger LOG = Logger.getLogger(CacheBootstrap.class);

    @Inject
    CachingConfiguration cachingConfiguration;

    @Inject
    Config config;

    @Inject
    Instance<RedisDataSource> redisDataSourceInstance;

    void onStart(@Observes StartupEvent ev) {
        LOG.info("=== Cache Configuration Bootstrap ===");
        
        // Check if cache configuration is present
        boolean cacheConfigPresent = isCacheConfigPresent();
        
        if (!cacheConfigPresent) {
            LOG.info("Cache configuration missing - caching disabled (no app.caching.* properties found)");
            LOG.info("=== Cache Bootstrap Complete ===");
            return;
        }
        
        // Log all caching-related configuration
        logCacheConfiguration();
        
        // If caching is disabled, skip all cache initialization
        if (!cachingConfiguration.isCachingEnabled()) {
            LOG.info("Caching is DISABLED (app.caching.enabled=false) - cache services will not be instantiated");
            LOG.info("=== Cache Bootstrap Complete ===");
            return;
        }
        
        LOG.info("Caching is ENABLED");
        
        // Check if Redis is configured
        if (cachingConfiguration.isRedisCache()) {
            LOG.info("Redis cache type configured - checking Redis health...");
            if (checkRedisHealth()) {
                LOG.info("✓ Redis is healthy - using Redis cache");
            } else {
                LOG.warn("✗ Redis is unhealthy - falling back to in-memory cache");
                // Override cache type to in-memory
                cachingConfiguration.overrideCacheType("in-memory");
            }
        } else if (cachingConfiguration.isInMemoryCache()) {
            LOG.info("In-memory cache type configured");
        } else {
            LOG.warnf("Unknown cache type '%s' - falling back to in-memory", cachingConfiguration.getCacheType());
            cachingConfiguration.overrideCacheType("in-memory");
        }
        
        LOG.info("=== Cache Bootstrap Complete ===");
    }
    
    /**
     * Checks if any cache configuration properties are present.
     * Returns true if at least one app.caching.* property is explicitly set.
     */
    private boolean isCacheConfigPresent() {
        return config.getOptionalValue("app.caching.enabled", String.class).isPresent()
            || config.getOptionalValue("app.caching.type", String.class).isPresent()
            || config.getOptionalValue("app.caching.tablefacility.ttl", String.class).isPresent();
    }

    private void logCacheConfiguration() {
        LOG.info("Cache Configuration:");
        logConfigProperty("app.caching.enabled", "false");
        logConfigProperty("app.caching.type", "in-memory");
        logConfigProperty("app.caching.tablefacility.ttl", "3600");
        
        // Log Redis config if present
        Optional<String> redisHost = config.getOptionalValue("quarkus.redis.hosts", String.class);
        if (redisHost.isPresent()) {
            LOG.infof("  quarkus.redis.hosts = %s", redisHost.get());
        } else {
            LOG.info("  quarkus.redis.hosts = (not configured)");
        }
    }
    
    private void logConfigProperty(String key, String defaultValue) {
        Optional<String> value = config.getOptionalValue(key, String.class);
        if (value.isPresent()) {
            LOG.infof("  %s = %s", key, value.get());
        } else {
            LOG.infof("  %s = %s (default)", key, defaultValue);
        }
    }

    private boolean checkRedisHealth() {
        if (!redisDataSourceInstance.isResolvable()) {
            LOG.warn("RedisDataSource is not available - Redis may not be configured");
            return false;
        }
        
        RedisDataSource redisDataSource = redisDataSourceInstance.get();
        try {
            KeyCommands<String> keyCommands = redisDataSource.key(String.class);
            // Perform a lightweight ping operation
            keyCommands.exists("__health_check__");
            LOG.info("Redis health check: SUCCESS");
            return true;
        } catch (Exception e) {
            LOG.errorf("Redis health check: FAILED - %s", e.getMessage());
            return false;
        }
    }
}

