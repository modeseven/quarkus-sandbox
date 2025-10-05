package org.acme.cache;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.acme.CiclopsResponse;
import org.acme.config.CachingConfiguration;
import org.acme.constants.CacheConstants;
import org.jboss.logging.Logger;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Service for caching response data, specifically for tablefacility fields.
 * This service extracts tablefacility fields from responses and caches them
 * with a generated cache key, then updates the response to include the cache
 * key.
 */
@ApplicationScoped
public class ResponseCacheService {

    private static final Logger LOG = Logger.getLogger(ResponseCacheService.class);

    @Inject
    CacheService cacheService;

    @Inject
    ObjectMapper objectMapper;

    @Inject
    CachingConfiguration cachingConfiguration;

    public CiclopsResponse processResponse(CiclopsResponse response, String trxId) {
        if (response == null || response.getFields() == null) {
            return response;
        }

        // Check if we should cache this response
        Map<String, List<String>> allFields = response.getFields();

        // We should cache if there are tablefacility fields
        boolean hasTablefacilityFields = allFields.keySet().stream()
                .anyMatch(key -> key.toLowerCase().startsWith(CacheConstants.TABLEFACILITY_PREFIX));

        // Only cache if there are tablefacility fields
        if (!hasTablefacilityFields) {
            LOG.debug("No tablefacility fields found, no caching needed");
            return response;
        }

        try {
            // Separate fields into tablefacility and non-tablefacility
            Map<String, List<String>> tablefacilityFields = new HashMap<>();
            Map<String, List<String>> nonTablefacilityFields = new HashMap<>();
            
            for (Map.Entry<String, List<String>> entry : allFields.entrySet()) {
                if (entry.getKey().toLowerCase().startsWith(CacheConstants.TABLEFACILITY_PREFIX)) {
                    tablefacilityFields.put(entry.getKey(), entry.getValue());
                } else {
                    nonTablefacilityFields.put(entry.getKey(), entry.getValue());
                }
            }

            LOG.debugf("Partitioned fields - tablefacility: %d, non-tablefacility: %d",
                    tablefacilityFields.size(), nonTablefacilityFields.size());
            LOG.debugf("Tablefacility fields: %s", tablefacilityFields.keySet());
            LOG.debugf("Non-tablefacility fields: %s", nonTablefacilityFields.keySet());

            // Generate cache key
            String cacheKey = generateCacheKey(trxId);

            // Serialize tablefacility fields to cache
            String serializedData = serializeFields(tablefacilityFields);

            // Cache the data with configured TTL
            cacheService.put(cacheKey, serializedData, cachingConfiguration.getTablefacilityTtlSeconds());

            LOG.infof("Cached %d fields (tablefacility) with key: %s", tablefacilityFields.size(), cacheKey);

            // Add cache key to the non-tablefacility fields
            List<String> cacheKeyValue = Arrays.asList(cacheKey);
            nonTablefacilityFields.put(CacheConstants.CACHE_KEY_FIELD, cacheKeyValue);

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
        return CacheConstants.CACHE_KEY_PREFIX + trxId + "_" + System.currentTimeMillis();
    }

    /**
     * Serializes all fields to JSON string
     */
    private String serializeFields(Map<String, List<String>> fields) throws JsonProcessingException {
        // Convert List<String> to single String values for serialization
        Map<String, String> serializableFields = fields.entrySet().stream()
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        entry -> entry.getValue().isEmpty() ? "" : entry.getValue().get(0)));

        return objectMapper.writeValueAsString(serializableFields);
    }

}
