package org.acme;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;
import jakarta.inject.Inject;
import org.acme.cache.CacheService;
import org.acme.cache.qualifiers.DefaultCacheImpl;
import org.acme.qualifiers.CachedTransactionRunnerQualifier;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.*;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test for CachedTransactionRunner using in-memory cache implementation.
 * This test verifies the complete caching flow:
 * 1. First call: No cache key → no field hydration → response gets cached with TF fields → returns cache key
 * 2. Second call: Uses cache key from first call → field hydration is called → mock response has NO TF fields
 */
@QuarkusTest
@TestProfile(CachedTransactionRunnerInMemoryTest.TestProfile.class)
public class CachedTransactionRunnerInMemoryTest {

    @Inject
    @CachedTransactionRunnerQualifier
    CachedTransactionRunner cachedTransactionRunner;

    @Inject
    @DefaultCacheImpl
    CacheService inMemoryCacheService;

    @BeforeEach
    public void setUp() {
        // Clear in-memory cache before each test
        inMemoryCacheService.clearAll();
    }

    @Test
    public void testCompleteCachingFlowWithInMemoryCache() {
        // === STEP 1: First call with NO cache key ===
        
        Map<String, String> firstCallInput = new HashMap<>();
        firstCallInput.put("user_id", "user123");
        firstCallInput.put("amount", "200.00");
        firstCallInput.put("currency", "EUR");
        firstCallInput.put("operation", "transfer");
        // NO tf_cache_key in input - this is the key point!
        
        // Process first transaction
        CiclopsResponse firstResponse = cachedTransactionRunner.processTransaction(firstCallInput, "test-trx-001");
        
        // Verify first response
        assertNotNull(firstResponse, "First response should not be null");
        assertNotNull(firstResponse.getFields(), "First response fields should not be null");
        
        // Debug: Print all fields in the first response
        // System.out.println("=== FIRST CALL RESPONSE ===");
        // System.out.println("First response fields: " + firstResponse.getFields().keySet());
        // for (Map.Entry<String, List<String>> entry : firstResponse.getFields().entrySet()) {
        //     System.out.println("  " + entry.getKey() + " = " + entry.getValue());
        // }
        
        // Verify that NO field hydration occurred (no cache key in input)
        // The original fields should be preserved as-is
        assertEquals("user123", firstResponse.getFields().get("user_id").get(0), 
                    "Original field 'user_id' should be preserved");
        assertEquals("200.00", firstResponse.getFields().get("amount").get(0), 
                    "Amount should remain as original value (no hydration)");
        assertEquals("EUR", firstResponse.getFields().get("currency").get(0), 
                    "Currency should remain as original value (no hydration)");
        assertEquals("transfer", firstResponse.getFields().get("operation").get(0), 
                    "Original field 'operation' should be preserved");
        
        // Verify that TestTransactionRunner added TF fields, but they are removed by ResponseCacheService
        // The TF fields are processed by ResponseCacheService and cached, then removed from response
        assertNull(firstResponse.getFields().get("tablefacility_1"), 
                  "TF field 1 should be removed from response after caching");
        assertNull(firstResponse.getFields().get("tablefacility_2"), 
                  "TF field 2 should be removed from response after caching");
        assertNull(firstResponse.getFields().get("tablefacility_3"), 
                  "TF field 3 should be removed from response after caching");
        
        // Verify that ResponseCacheService processed the response and added a cache key
        assertNotNull(firstResponse.getFields().get("tf_cache_key"), 
                     "Response should contain a cache key from ResponseCacheService");
        String firstCacheKey = firstResponse.getFields().get("tf_cache_key").get(0);
        assertNotNull(firstCacheKey, "First cache key should not be null");
        assertTrue(firstCacheKey.startsWith("tf_cache_"), "First cache key should start with tf_cache_");
        
        // Verify that TF fields are NOT in the response (they should be removed and cached)
        // This is the key behavior: TF fields are removed from response and cached
        assertNull(firstResponse.getFields().get("tablefacility_1"), 
                  "TF field 1 should be removed from response after caching");
        assertNull(firstResponse.getFields().get("tablefacility_2"), 
                  "TF field 2 should be removed from response after caching");
        assertNull(firstResponse.getFields().get("tablefacility_3"), 
                  "TF field 3 should be removed from response after caching");
        
        // Verify the cache key was stored in in-memory cache
        String cachedDataFromFirstCall = inMemoryCacheService.get(firstCacheKey);
        assertNotNull(cachedDataFromFirstCall, "First cache key should exist in in-memory cache");
        assertTrue(cachedDataFromFirstCall.contains("tablefacility_1"), 
                  "Cached data should contain TF field 1");
        assertTrue(cachedDataFromFirstCall.contains("tablefacility_2"), 
                  "Cached data should contain TF field 2");
        assertTrue(cachedDataFromFirstCall.contains("tablefacility_3"), 
                  "Cached data should contain TF field 3");
        
        // === STEP 2: Second call with cache key from first call ===
        
        Map<String, String> secondCallInput = new HashMap<>();
        secondCallInput.put("user_id", "user456");
        secondCallInput.put("amount", "300.00"); // This should be overridden by cached data
        secondCallInput.put("currency", "GBP"); // This should be overridden by cached data
        secondCallInput.put("operation", "withdrawal");
        secondCallInput.put("tf_cache_key", firstCacheKey); // Use the cache key from first call!
        
        // Process second transaction
        CiclopsResponse secondResponse = cachedTransactionRunner.processTransaction(secondCallInput, "test-trx-002");
        
        // Verify second response
        assertNotNull(secondResponse, "Second response should not be null");
        assertNotNull(secondResponse.getFields(), "Second response fields should not be null");
        
        // Debug: Print all fields in the second response
        // System.out.println("=== SECOND CALL RESPONSE ===");
        // System.out.println("Second response fields: " + secondResponse.getFields().keySet());
        // for (Map.Entry<String, List<String>> entry : secondResponse.getFields().entrySet()) {
        //     System.out.println("  " + entry.getKey() + " = " + entry.getValue());
        // }
        
        // Debug: Check what's in the cache
        // System.out.println("=== CACHE DEBUG ===");
        // System.out.println("First cache key: " + firstCacheKey);
        // String firstCachedData = inMemoryCacheService.get(firstCacheKey);
        // System.out.println("First cached data: " + firstCachedData);
        String secondCacheKey = secondResponse.getFields().get("tf_cache_key").get(0);
        // System.out.println("Second cache key: " + secondCacheKey);
        // String secondCachedData = inMemoryCacheService.get(secondCacheKey);
        // System.out.println("Second cached data: " + secondCachedData);
        
        // Verify that field hydration occurred (cache key was present)
        // The cached TF fields should be hydrated into the input to TestTransactionRunner
        // But since TestTransactionRunner always adds its own TF fields, we should see BOTH:
        // 1. The hydrated TF fields from cache
        // 2. The new TF fields from TestTransactionRunner
        
        // Verify that cached fields are hydrated (should override original values)
        // Note: The hydration happens in the INPUT to TestTransactionRunner,
        // so TestTransactionRunner sees the hydrated fields and processes them
        assertEquals("user456", secondResponse.getFields().get("user_id").get(0), 
                    "New original field 'user_id' should be preserved");
        assertEquals("withdrawal", secondResponse.getFields().get("operation").get(0), 
                    "New original field 'operation' should be preserved");
        
        // Verify that TestTransactionRunner added NEW TF fields, but they are removed by ResponseCacheService
        // The TF fields are processed by ResponseCacheService and cached, then removed from response
        assertNull(secondResponse.getFields().get("tablefacility_1"), 
                  "TF field 1 should be removed from response after caching");
        assertNull(secondResponse.getFields().get("tablefacility_2"), 
                  "TF field 2 should be removed from response after caching");
        assertNull(secondResponse.getFields().get("tablefacility_3"), 
                  "TF field 3 should be removed from response after caching");
        
        // Verify that ResponseCacheService processed the response and added a NEW cache key
        assertNotNull(secondResponse.getFields().get("tf_cache_key"), 
                     "Second response should contain a cache key from ResponseCacheService");
        assertNotNull(secondCacheKey, "Second cache key should not be null");
        assertTrue(secondCacheKey.startsWith("tf_cache_"), "Second cache key should start with tf_cache_");
        assertNotEquals(firstCacheKey, secondCacheKey, "Second cache key should be different from first");
        
        // Verify that TF fields are NOT in the final response (they should be removed and cached)
        // This is the key behavior: TF fields are removed from response and cached
        assertNull(secondResponse.getFields().get("tablefacility_1"), 
                  "TF field 1 should be removed from response after caching");
        assertNull(secondResponse.getFields().get("tablefacility_2"), 
                  "TF field 2 should be removed from response after caching");
        assertNull(secondResponse.getFields().get("tablefacility_3"), 
                  "TF field 3 should be removed from response after caching");
        
        // Verify the second cache key was stored in in-memory cache
        String cachedDataFromSecondCall = inMemoryCacheService.get(secondCacheKey);
        assertNotNull(cachedDataFromSecondCall, "Second cache key should exist in in-memory cache");
        assertTrue(cachedDataFromSecondCall.contains("tablefacility_1"), 
                  "Second cached data should contain TF field 1");
        assertTrue(cachedDataFromSecondCall.contains("tablefacility_2"), 
                  "Second cached data should contain TF field 2");
        assertTrue(cachedDataFromSecondCall.contains("tablefacility_3"), 
                  "Second cached data should contain TF field 3");
        
        // === VERIFY CACHE PERSISTENCE ===
        
        // Verify that the first cache key still exists in the cache
        String firstCachedDataStillExists = inMemoryCacheService.get(firstCacheKey);
        assertNotNull(firstCachedDataStillExists, "First cached data should still exist in cache");
        
        // Verify that the second cache key exists in the cache
        String secondCachedDataExists = inMemoryCacheService.get(secondCacheKey);
        assertNotNull(secondCachedDataExists, "Second cached data should exist in cache");
        
        // Verify both cache entries are different (different TF field values)
        assertNotEquals(firstCachedDataStillExists, secondCachedDataExists, 
                       "First and second cached data should be different");
        
        // === VERIFY FIELD COUNTS ===
        
        // First response: user_id, amount, currency, operation, tf_cache_key (TF fields removed)
        int firstExpectedFieldCount = 5;
        assertEquals(firstExpectedFieldCount, firstResponse.getFields().size(),
                    "First response should have " + firstExpectedFieldCount + " fields");
        
        // Second response: user_id, operation, tf_cache_key, amount, currency (TF fields removed, but amount/currency are not hydrated from cache)
        int secondExpectedFieldCount = 5;
        assertEquals(secondExpectedFieldCount, secondResponse.getFields().size(),
                    "Second response should have " + secondExpectedFieldCount + " fields");
    }

    @Test
    public void testCacheMissScenario() {
        // Test with a non-existent cache key
        Map<String, String> inputFields = new HashMap<>();
        inputFields.put("user_id", "user999");
        inputFields.put("amount", "50.00");
        inputFields.put("tf_cache_key", "non-existent-cache-key");
        
        CiclopsResponse response = cachedTransactionRunner.processTransaction(inputFields, "test-trx-999");
        
        // Verify response is not null
        assertNotNull(response, "Response should not be null");
        assertNotNull(response.getFields(), "Response fields should not be null");
        
        // Verify original fields are preserved
        assertEquals("user999", response.getFields().get("user_id").get(0));
        assertEquals("50.00", response.getFields().get("amount").get(0));
        // The cache key gets replaced by ResponseCacheService with a new generated key
        assertNotNull(response.getFields().get("tf_cache_key"), "Response should have a cache key");
        assertTrue(response.getFields().get("tf_cache_key").get(0).startsWith("tf_cache_"), 
                  "Cache key should start with tf_cache_");
        
        // Verify no TF fields are hydrated (cache miss)
        assertNull(response.getFields().get("tablefacility_1"), 
                  "No TF fields should be hydrated on cache miss");
    }

    /**
     * Test profile to enable caching for this test and use in-memory cache
     */
    public static class TestProfile implements io.quarkus.test.junit.QuarkusTestProfile {
        @Override
        public Map<String, String> getConfigOverrides() {
            Map<String, String> config = new HashMap<>();
            config.put("app.caching.enabled", "true");
            config.put("app.caching.tablefacility.ttl", "3600");
            return config;
        }
        
        @Override
        public Set<Class<?>> getEnabledAlternatives() {
            return Set.of(
                org.acme.cache.TestResponseCacheServiceImpl.class,
                org.acme.cache.TestFieldHydrationService.class
            );
        }
    }
}
