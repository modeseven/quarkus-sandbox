package org.acme;

import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.acme.cache.CacheService;
import org.acme.cache.qualifiers.DefaultCacheImpl;
import org.acme.qualifiers.CachedTransactionRunnerQualifier;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

/**
 * Demonstration of how to test the CachedTransactionRunner
 * This shows the complete flow: cache population -> cache hit -> field hydration
 */
@QuarkusTest
public class CacheDemo {

    @Inject
    @CachedTransactionRunnerQualifier
    CachedTransactionRunner cachedTransactionRunner;

    @Inject
    @DefaultCacheImpl
    CacheService cacheService;

    @Inject
    CachePopulator cachePopulator;

    @Test
    public void demonstrateCacheHitBehavior() {
        System.out.println("=== CachedTransactionRunner Demo ===\n");
        
        // Step 1: Clear any existing cache
        cachePopulator.clearCache();
        System.out.println("1. Cache cleared");
        
        // Step 2: Populate cache with test data
        cachePopulator.populateCacheWithTestData();
        System.out.println("2. Cache populated with test data for key: test-cache-key-123");
        
        // Step 3: Create input fields with cache key
        Map<String, String> inputFields = new HashMap<>();
        inputFields.put("tf_cache_key", "test-cache-key-123");
        inputFields.put("amount", "50.00");      // This will be overridden by cache
        inputFields.put("currency", "EUR");      // This will be overridden by cache  
        inputFields.put("user_id", "user123");   // This will be preserved
        inputFields.put("custom_field", "custom_value"); // This will be preserved
        
        System.out.println("3. Input fields:");
        inputFields.forEach((key, value) -> System.out.println("   " + key + " = " + value));
        
        // Step 4: Process transaction - should hit cache and hydrate fields
        System.out.println("\n4. Processing transaction with cache key...");
        CiclopsResponse response = cachedTransactionRunner.processTransaction(inputFields, "demo-trx-001");
        
        // Step 5: Show the hydrated fields
        System.out.println("\n5. Fields after cache hit and hydration:");
        response.getFields().forEach((key, value) -> {
            String source = inputFields.containsKey(key) ? 
                (inputFields.get(key).equals(value.get(0)) ? "(original)" : "(overridden by cache)") : 
                "(added from cache)";
            System.out.println("   " + key + " = " + value.get(0) + " " + source);
        });
        
        // Step 6: Demonstrate cache miss scenario
        System.out.println("\n6. Testing cache miss scenario...");
        Map<String, String> missFields = new HashMap<>();
        missFields.put("tf_cache_key", "non-existent-key");
        missFields.put("amount", "75.00");
        missFields.put("currency", "GBP");
        missFields.put("user_id", "user456");
        
        System.out.println("   Input fields for cache miss:");
        missFields.forEach((key, value) -> System.out.println("     " + key + " = " + value));
        
        CiclopsResponse missResponse = cachedTransactionRunner.processTransaction(missFields, "demo-trx-002");
        
        System.out.println("   Fields after cache miss (no hydration):");
        missResponse.getFields().forEach((key, value) -> 
            System.out.println("     " + key + " = " + value.get(0) + " (original)"));
        
        System.out.println("\n=== Demo Complete ===");
    }
}
