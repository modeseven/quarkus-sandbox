package org.acme.cache;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Inject;
import org.acme.cache.qualifiers.DefaultCacheImpl;
import org.acme.cache.qualifiers.RedisCacheImpl;
import org.acme.config.CachingConfiguration;
import org.jboss.logging.Logger;

/**
 * Producer for CacheService that conditionally provides either Redis or In-Memory implementation
 * based on configuration.
 */
@ApplicationScoped
public class CacheServiceProducer {

    private static final Logger LOG = Logger.getLogger(CacheServiceProducer.class);

    @Inject
    CachingConfiguration cachingConfiguration;

    @Inject
    @RedisCacheImpl
    CacheService redisCacheService;

    @Inject
    @DefaultCacheImpl
    CacheService inMemoryCacheService;

    /**
     * Produces the appropriate CacheService implementation based on configuration.
     * 
     * @return The configured cache service implementation
     */
    @Produces
    @ApplicationScoped
    public CacheService produceCacheService() {
        String cacheType = cachingConfiguration.getCacheType();
        
        LOG.infof("Configuring cache service with type: %s", cacheType);
        
        if (cachingConfiguration.isRedisCache()) {
            LOG.info("Using Redis cache implementation");
            return redisCacheService;
        } else if (cachingConfiguration.isInMemoryCache()) {
            LOG.info("Using In-Memory cache implementation");
            return inMemoryCacheService;
        } else {
            LOG.warnf("Unknown cache type '%s', falling back to in-memory cache", cacheType);
            return inMemoryCacheService;
        }
    }
}
