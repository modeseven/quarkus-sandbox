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
    private static final String CACHE_KEY_PREFIX = "TF_CACHE_";
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

        // We should cache if there are tablefacility fields
        boolean hasTablefacilityFields = allFields.keySet().stream()
                .anyMatch(key -> key.toLowerCase().startsWith(TABLEFACILITY_PREFIX));

        // Only cache if there are tablefacility fields
        if (!hasTablefacilityFields) {
            LOG.debug("No tablefacility fields found, no caching needed");
            return response;
        }

        try {
            // Partition fields into tablefacility and non-tablefacility in one operation
            Map<Boolean, Map<String, List<String>>> partitionedFields = allFields.entrySet().stream()
                    .collect(Collectors.partitioningBy(
                            entry -> entry.getKey().toLowerCase().startsWith(TABLEFACILITY_PREFIX),
                            Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue)
                    ));

            Map<String, List<String>> tablefacilityFields = partitionedFields.get(true);
            Map<String, List<String>> nonTablefacilityFields = partitionedFields.get(false);

            // Generate cache key
            String cacheKey = generateCacheKey(trxId);

            // Serialize tablefacility fields to cache
            String serializedData = serializeFields(tablefacilityFields);

            // Cache the data with configured TTL
            cacheService.put(cacheKey, serializedData, cachingConfiguration.getTablefacilityTtlSeconds());

            LOG.infof("Cached %d fields (tablefacility) with key: %s", tablefacilityFields.size(), cacheKey);

            // Add cache key to the non-tablefacility fields
            List<String> cacheKeyValue = Arrays.asList(cacheKey);
            nonTablefacilityFields.put("tf_cache_key", cacheKeyValue);

            // Update the response fields
            response.setFields(nonTablefacilityFields);
            return response;

        } catch (Exception e) {
            LOG.errorf("Error processing response for caching: %s", e.getMessage());
            // Return original response if caching fails
            return response;
        }
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

}
