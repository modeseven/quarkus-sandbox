package org.acme;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.acme.cache.CacheService;
import org.acme.cache.qualifiers.RedisCacheImpl;
import org.jboss.logging.Logger;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Utility class to populate cache with test data
 */
@ApplicationScoped
public class CachePopulator {

    private static final Logger LOG = Logger.getLogger(CachePopulator.class);
    private static final ObjectMapper objectMapper = new ObjectMapper();

    @Inject
    @RedisCacheImpl
    CacheService cacheService;

    /**
     * Populate cache with sample data for testing
     */
    public void populateCacheWithTestData() {
        try {
            // Sample cached data that would be returned for cache key "test-cache-key-123"
            Map<String, String> cachedData = new HashMap<>();
            cachedData.put("tf_cache_key", "test-cache-key-123");
            cachedData.put("amount", "100.50");
            cachedData.put("currency", "USD");
            cachedData.put("timestamp", "1703123456789");
            cachedData.put("status", "processed");
            cachedData.put("facility_id", "facility_001");
            cachedData.put("tablefacility_2", "sample_data_2");
            cachedData.put("tablefacility_3", "sample_data_3");
            cachedData.put("tablefacility_4", "sample_data_4");

            // Serialize the data
            String serializedData = objectMapper.writeValueAsString(cachedData);
            
            // Store in cache with 1 hour TTL
            cacheService.put("test-cache-key-123", serializedData, 3600);
            
            LOG.info("Cache populated with test data for key: test-cache-key-123");
            
        } catch (JsonProcessingException e) {
            LOG.errorf("Failed to populate cache with test data: %s", e.getMessage());
        }
    }

    /**
     * Clear all cache data
     */
    public void clearCache() {
        cacheService.clearAll();
        LOG.info("Cache cleared");
    }
}
