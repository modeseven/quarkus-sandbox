package org.acme.cache;

import org.acme.CiclopsResponse;
import java.util.Map;

/**
 * Service for caching response data, specifically for tablefacility fields.
 * This service extracts tablefacility fields from responses and caches them
 * with a generated cache key, then updates the response to include the cache key.
 */
public interface ResponseCacheService {

    /**
     * Processes a response to extract and cache tablefacility fields.
     * If any tablefacility fields are found, they are extracted, serialized,
     * and cached. The response is then updated to include the cache key and
     * the tablefacility fields are removed from the response.
     * 
     * @param response The response to process
     * @param trxId The transaction ID for cache key generation
     * @return The processed response with cache key if applicable
     */
    CiclopsResponse processResponse(CiclopsResponse response, String trxId);
}
