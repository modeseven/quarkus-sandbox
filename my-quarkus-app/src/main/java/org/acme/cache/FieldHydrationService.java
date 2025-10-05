package org.acme.cache;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.acme.config.CachingConfiguration;
import org.acme.constants.CacheConstants;
import org.jboss.logging.Logger;

import java.util.HashMap;
import java.util.Map;

/**
 * Service responsible for hydrating input fields with cached data.
 * This service handles the logic of merging cached data into input fields.
 */
@ApplicationScoped
public class FieldHydrationService {

    private static final Logger LOG = Logger.getLogger(FieldHydrationService.class);
    private static final ObjectMapper objectMapper = new ObjectMapper();

    @Inject
    CacheService cacheService;

    @Inject
    CachingConfiguration cachingConfiguration;

    /**
     * Hydrates input fields with cached data if available.
     * 
     * @param fields The original input fields
     * @return Enhanced fields with cached data merged in, or original fields if no cache hit
     */
    public Map<String, String> hydrateFields(Map<String, String> fields) {
        // Check if fields contain a cache key
        String cacheKey = discoverCacheKey(fields);
        if (cacheKey == null) {
            LOG.debug("No cache key found, returning original fields");
            return fields;
        }

        // Attempt to retrieve cached data
        String cachedValue = retrieveFromCache(cacheKey);
        if (cachedValue == null) {
            LOG.debugf("Cache miss for key: %s, returning original fields", cacheKey);
            return fields;
        }

        // Process cache hit and hydrate fields
        LOG.infof("Cache hit for key: %s, hydrating fields", cacheKey);
        return processCacheHit(fields, cachedValue);
    }


    /**
     * Discover the cache key from the input fields
     */
    private String discoverCacheKey(Map<String, String> fields) {
        if (fields == null || !fields.containsKey(CacheConstants.CACHE_KEY_FIELD)) {
            return null;
        }
        return fields.get(CacheConstants.CACHE_KEY_FIELD);
    }

    /**
     * Attempt to retrieve value from cache
     */
    private String retrieveFromCache(String cacheKey) {
        try {
            return cacheService.get(cacheKey);
        } catch (Exception e) {
            LOG.errorf("Error retrieving from cache for key '%s': %s", cacheKey, e.getMessage());
            return null;
        }
    }

    /**
     * Process a cache hit - deserialize and merge cached data with original fields
     */
    private Map<String, String> processCacheHit(Map<String, String> originalFields, String cachedValue) {
        try {
            // Deserialize cached data
            Map<String, String> cachedDataMap = deserializeCachedData(cachedValue);
            if (cachedDataMap == null) {
                LOG.warn("Failed to deserialize cached data, returning original fields");
                return originalFields;
            }

            // Merge fields
            return mergeFields(originalFields, cachedDataMap);

        } catch (Exception e) {
            LOG.errorf("Error processing cache hit: %s", e.getMessage());
            return originalFields;
        }
    }

    /**
     * Deserialize the cached JSON string back to Map<String, String>
     */
    private Map<String, String> deserializeCachedData(String cachedValue) {
        try {
            return objectMapper.readValue(cachedValue, 
                objectMapper.getTypeFactory().constructMapType(Map.class, String.class, String.class));
        } catch (JsonProcessingException e) {
            LOG.errorf("Failed to deserialize cached data: %s", e.getMessage());
            return null;
        }
    }

    /**
     * Merge cached data with original fields to create enhanced input
     */
    private Map<String, String> mergeFields(Map<String, String> originalFields, Map<String, String> cachedDataMap) {
        // Create a new map starting with the original fields
        Map<String, String> enhancedFields = new HashMap<>(originalFields);
        
        // Merge in the cached data (cached data takes precedence for overlapping keys)
        enhancedFields.putAll(cachedDataMap);
        
        LOG.debugf("Enhanced fields with %d cached entries, total fields: %d", 
                   cachedDataMap.size(), enhancedFields.size());
        
        return enhancedFields;
    }
}
