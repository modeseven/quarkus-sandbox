package org.acme.cache;

import io.quarkus.redis.datasource.RedisDataSource;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Instance;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Inject;
import org.acme.config.CachingConfiguration;
import org.jboss.logging.Logger;

/**
 * Producer for CacheService that conditionally provides either Redis, In-Memory, or NoOp implementation
 * based on configuration. When caching is disabled, returns NoOpCacheService (feature lays dormant).
 */
@ApplicationScoped
public class CacheServiceProducer {

    private static final Logger LOG = Logger.getLogger(CacheServiceProducer.class);

    @Inject
    CachingConfiguration cachingConfiguration;

    // Inject Instance lazily - only resolve when actually needed
    @Inject
    Instance<RedisDataSource> redisDataSourceInstance;

    // Cache instance to ensure singleton behavior
    private CacheService activeCacheService;
    private final NoOpCacheService noOpCacheService = new NoOpCacheService();

    /**
     * Produces the appropriate CacheService implementation based on configuration.
     * Returns NoOpCacheService when caching is disabled (feature lays dormant).
     * 
     * @return The configured cache service implementation
     */
    @Produces
    @ApplicationScoped
    public CacheService produceCacheService() {
        // If already created, return cached instance
        if (activeCacheService != null) {
            return activeCacheService;
        }

        // If caching is disabled, return NoOp (feature is dormant)
        if (!cachingConfiguration.isCachingEnabled()) {
            LOG.debug("Caching is disabled - returning NoOp cache service (feature dormant)");
            activeCacheService = noOpCacheService;
            return noOpCacheService;
        }

        // Caching is enabled - create the appropriate cache service
        String cacheType = cachingConfiguration.getCacheType();
        LOG.infof("Caching is enabled - configuring cache service with type: %s", cacheType);

        if (cachingConfiguration.isRedisCache()) {
            // Try to create Redis cache service
            if (redisDataSourceInstance.isResolvable()) {
                LOG.info("Creating Redis cache service");
                RedisCacheService service = new RedisCacheService(redisDataSourceInstance.get());
                service.init();
                activeCacheService = service;
                return service;
            } else {
                LOG.warn("Redis configured but RedisDataSource not available - falling back to in-memory");
                // Fall through to in-memory
            }
        }

        // Use in-memory cache (either configured or as fallback)
        LOG.info("Creating In-memory cache service");
        InMemoryCacheService service = new InMemoryCacheService();
        service.initCleanupTask();
        activeCacheService = service;
        return service;
    }
}
