package org.acme.constants;

/**
 * Constants related to caching functionality.
 * Centralizes all cache-related string constants to avoid duplication
 * and ensure consistency across the application.
 */
public final class CacheConstants {
    
    /**
     * The field name used to store cache keys in responses and input fields.
     * This field is used by both FieldHydrationService and ResponseCacheService.
     */
    public static final String CACHE_KEY_FIELD = "tf_cache_key";
    
    /**
     * Prefix used when generating cache keys for tablefacility data.
     */
    public static final String CACHE_KEY_PREFIX = "TF_CACHE_";
    
    /**
     * Prefix used to identify tablefacility fields that should be cached.
     */
    public static final String TABLEFACILITY_PREFIX = "tablefacility";
    
    /**
     * Private constructor to prevent instantiation of this utility class.
     */
    private CacheConstants() {
        // Utility class - no instantiation allowed
    }
}
