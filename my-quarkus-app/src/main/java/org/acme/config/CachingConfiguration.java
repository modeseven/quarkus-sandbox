package org.acme.config;

import jakarta.enterprise.context.ApplicationScoped;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

/**
 * Configuration service for caching settings
 */
@ApplicationScoped
public class CachingConfiguration {

    private static final Logger LOG = Logger.getLogger(CachingConfiguration.class);

    @ConfigProperty(name = "app.caching.enabled", defaultValue = "false")
    boolean cachingEnabled;

    @ConfigProperty(name = "app.caching.tablefacility.ttl", defaultValue = "3600")
    int tablefacilityTtlSeconds;

    @ConfigProperty(name = "app.caching.type", defaultValue = "in-memory")
    String cacheType;

    // Runtime override for cache type (e.g., when Redis is unhealthy)
    private String overrideCacheType;

    public boolean isCachingEnabled() {
        return cachingEnabled;
    }

    public int getTablefacilityTtlSeconds() {
        return tablefacilityTtlSeconds;
    }

    public String getCacheType() {
        return overrideCacheType != null ? overrideCacheType : cacheType;
    }

    /**
     * Override the cache type at runtime (e.g., when Redis is unhealthy).
     */
    public void overrideCacheType(String newCacheType) {
        LOG.infof("Overriding cache type from '%s' to '%s'", cacheType, newCacheType);
        this.overrideCacheType = newCacheType;
    }

    public boolean isRedisCache() {
        return "redis".equalsIgnoreCase(getCacheType());
    }

    public boolean isInMemoryCache() {
        return "in-memory".equalsIgnoreCase(getCacheType());
    }
}
