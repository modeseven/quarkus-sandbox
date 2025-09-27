package org.acme.cache;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Alternative;
import jakarta.inject.Inject;
import org.acme.CiclopsResponse;
import org.acme.cache.qualifiers.DefaultCacheImpl;
import org.acme.config.CachingConfiguration;
import org.jboss.logging.Logger;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Test implementation of ResponseCacheService that uses in-memory cache.
 * This is used for testing to avoid Redis dependency.
 */
@ApplicationScoped
@Alternative
public class TestResponseCacheServiceImpl {

    private static final Logger LOG = Logger.getLogger(TestResponseCacheServiceImpl.class);
    private static final String CACHE_KEY_PREFIX = "tf_cache_";
    private static final String TABLEFACILITY_PREFIX = "tablefacility";

    @Inject
    @DefaultCacheImpl
    CacheService cacheService;

    @Inject
    ObjectMapper objectMapper;

    @Inject
    CachingConfiguration cachingConfiguration;

    public CiclopsResponse processResponse(CiclopsResponse response, String trxId) {
        if (response == null || response.getFields() == null) {
            return response;
        }

        // Check if caching is enabled
        if (!cachingConfiguration.isCachingEnabled()) {
            LOG.debug("Caching is disabled, skipping tablefacility field caching");
            return response;
        }

        // Check if we should cache this response
        Map<String, List<String>> allFields = response.getFields();
        
        // We should cache if there are tablefacility fields OR if there are hydrated fields
        boolean hasTablefacilityFields = allFields.keySet().stream()
                .anyMatch(key -> key.toLowerCase().startsWith(TABLEFACILITY_PREFIX));
        
        boolean hasHydratedFields = allFields.keySet().stream()
                .anyMatch(key -> key.equals("status") || key.equals("facility_id") || 
                               key.equals("amount") || key.equals("currency") || 
                               key.equals("timestamp"));
        
        // Only cache if there are tablefacility fields OR if there are hydrated fields AND a cache key was provided
        boolean hasCacheKey = allFields.containsKey("tf_cache_key");
        
        if (!hasTablefacilityFields && (!hasHydratedFields || !hasCacheKey)) {
            LOG.debug("No tablefacility fields found and no cache key provided, no caching needed");
            return response;
        }

        // Extract tablefacility fields
        Map<String, List<String>> tablefacilityFields = extractTablefacilityFields(allFields);
        
        // Extract hydrated fields (fields that were likely hydrated from cache)
        Map<String, List<String>> hydratedFields = extractHydratedFields(allFields);

        LOG.debugf("Found %d tablefacility fields and %d hydrated fields, processing for caching", 
                   tablefacilityFields.size(), hydratedFields.size());

        try {
            // Generate cache key
            String cacheKey = generateCacheKey(trxId);
            
            // Combine tablefacility and hydrated fields for caching
            Map<String, List<String>> fieldsToCache = new HashMap<>();
            fieldsToCache.putAll(tablefacilityFields);
            fieldsToCache.putAll(hydratedFields);
            
            // Serialize all fields to cache
            String serializedData = serializeFields(fieldsToCache);
            
            // Cache the data with configured TTL
            cacheService.put(cacheKey, serializedData, cachingConfiguration.getTablefacilityTtlSeconds());
            
            LOG.infof("Cached %d fields (tablefacility + hydrated) with key: %s", fieldsToCache.size(), cacheKey);
            
            // Create new response without tablefacility fields but with cache key
            return createResponseWithCacheKey(allFields, tablefacilityFields, cacheKey);
            
        } catch (Exception e) {
            LOG.errorf("Error processing response for caching: %s", e.getMessage());
            // Return original response if caching fails
            return response;
        }
    }


    /**
     * Extracts fields that start with the tablefacility prefix
     */
    private Map<String, List<String>> extractTablefacilityFields(Map<String, List<String>> allFields) {
        return allFields.entrySet().stream()
                .filter(entry -> entry.getKey().toLowerCase().startsWith(TABLEFACILITY_PREFIX))
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        Map.Entry::getValue
                ));
    }

    /**
     * Extracts hydrated fields (fields that were likely hydrated from cache)
     */
    private Map<String, List<String>> extractHydratedFields(Map<String, List<String>> allFields) {
        return allFields.entrySet().stream()
                .filter(entry -> {
                    String key = entry.getKey();
                    return key.equals("status") || key.equals("facility_id") || 
                           key.equals("amount") || key.equals("currency") || 
                           key.equals("timestamp");
                })
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        Map.Entry::getValue
                ));
    }

    /**
     * Generates a unique cache key for the transaction
     */
    private String generateCacheKey(String trxId) {
        return CACHE_KEY_PREFIX + trxId + "_" + System.currentTimeMillis();
    }

    /**
     * Serializes all fields to JSON string
     */
    private String serializeFields(Map<String, List<String>> fields) throws JsonProcessingException {
        // Convert List<String> to single String values for serialization
        Map<String, String> serializableFields = fields.entrySet().stream()
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        entry -> entry.getValue().isEmpty() ? "" : entry.getValue().get(0)
                ));
        
        return objectMapper.writeValueAsString(serializableFields);
    }


    /**
     * Creates a new response with tablefacility fields removed and cache key added
     */
    private CiclopsResponse createResponseWithCacheKey(Map<String, List<String>> allFields, 
                                                       Map<String, List<String>> tablefacilityFields, 
                                                       String cacheKey) {
        // Create new fields map without tablefacility fields
        Map<String, List<String>> filteredFields = allFields.entrySet().stream()
                .filter(entry -> !tablefacilityFields.containsKey(entry.getKey()))
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        Map.Entry::getValue
                ));

        // Add cache key to the response
        List<String> cacheKeyValue = Arrays.asList(cacheKey);
        filteredFields.put("tf_cache_key", cacheKeyValue);

        return new CiclopsResponse(filteredFields);
    }

}
