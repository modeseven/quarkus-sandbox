package org.acme;

import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.acme.cache.CacheService;
import org.acme.cache.qualifiers.RedisCacheImpl;
import org.acme.qualifiers.CachedTransactionRunnerQualifier;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@QuarkusTest
public class CachedTransactionRunnerTest {

    @Inject
    @CachedTransactionRunnerQualifier
    CachedTransactionRunner cachedTransactionRunner;

    @Inject
    @RedisCacheImpl
    CacheService cacheService;

    @Inject
    CachePopulator cachePopulator;

    @BeforeEach
    public void setUp() {
        // Clear cache before each test
        cachePopulator.clearCache();
    }

    @Test
    public void testCacheHitWithFieldHydration() {
        // Step 1: Populate cache with test data
        cachePopulator.populateCacheWithTestData();
        
        // Step 2: Create input fields with cache key
        Map<String, String> inputFields = new HashMap<>();
        inputFields.put("tf_cache_key", "test-cache-key-123");
        inputFields.put("amount", "50.00");  // This will be overridden by cached data
        inputFields.put("currency", "EUR"); // This will be overridden by cached data
        inputFields.put("user_id", "user123"); // This will remain as it's not in cache
        
        // Step 3: Process transaction - should hit cache
        CiclopsResponse response = cachedTransactionRunner.processTransaction(inputFields, "test-trx-001");
        
        // Step 4: Verify response contains hydrated fields
        assertNotNull(response, "Response should not be null");
        assertNotNull(response.getFields(), "Response fields should not be null");
        
        // Verify original fields are preserved
        assertEquals("user123", response.getFields().get("user_id").get(0), 
                    "Original field 'user_id' should be preserved");
        
        // Verify cached fields are hydrated (should override original values)
        assertEquals("100.50", response.getFields().get("amount").get(0), 
                    "Amount should be hydrated from cache (100.50)");
        assertEquals("USD", response.getFields().get("currency").get(0), 
                    "Currency should be hydrated from cache (USD)");
        assertEquals("1703123456789", response.getFields().get("timestamp").get(0), 
                    "Timestamp should be hydrated from cache");
        
        // Verify additional cached fields are present
        assertEquals("processed", response.getFields().get("status").get(0), 
                    "Status should be hydrated from cache");
        assertEquals("facility_001", response.getFields().get("facility_id").get(0), 
                    "Facility ID should be hydrated from cache");
        
        // Verify cache key is present (but it's been updated to a new generated key)
        assertNotNull(response.getFields().get("tf_cache_key"), 
                    "Cache key should be present");
        assertTrue(response.getFields().get("tf_cache_key").get(0).startsWith("tf_cache_"), 
                    "Cache key should start with 'tf_cache_'");
        
        // Verify total number of fields (original + cached)
        int expectedFieldCount = 7; // user_id + cached fields + tf_cache_key
        assertEquals(expectedFieldCount, response.getFields().size(), 
                    "Should have " + expectedFieldCount + " fields after hydration");
        
        // === STEP 5: Second call with the new cache key (should get same hydrated data) ===
        
        // Get the new cache key from the first response
        String newCacheKey = response.getFields().get("tf_cache_key").get(0);
        
        // Create input for second call using the new cache key
        Map<String, String> secondCallInput = new HashMap<>();
        secondCallInput.put("tf_cache_key", newCacheKey);
        secondCallInput.put("amount", "999.99"); // This should be overridden by cached data
        secondCallInput.put("currency", "JPY"); // This should be overridden by cached data
        secondCallInput.put("user_id", "user456"); // This should remain as it's not in cache
        
        // Process second transaction - should hit cache again
        CiclopsResponse secondResponse = cachedTransactionRunner.processTransaction(secondCallInput, "test-trx-002");
        
        // Verify second response contains same hydrated data
        assertNotNull(secondResponse, "Second response should not be null");
        assertNotNull(secondResponse.getFields(), "Second response fields should not be null");
        
        // Verify cached fields are hydrated again (same cached data)
        assertEquals("100.50", secondResponse.getFields().get("amount").get(0), 
                    "Amount should be hydrated from cache again");
        assertEquals("USD", secondResponse.getFields().get("currency").get(0), 
                    "Currency should be hydrated from cache again");
        assertEquals("1703123456789", secondResponse.getFields().get("timestamp").get(0), 
                    "Timestamp should be hydrated from cache again");
        assertEquals("processed", secondResponse.getFields().get("status").get(0), 
                    "Status should be hydrated from cache again");
        assertEquals("facility_001", secondResponse.getFields().get("facility_id").get(0), 
                    "Facility ID should be hydrated from cache again");
        
        // Verify new original fields are preserved
        assertEquals("user456", secondResponse.getFields().get("user_id").get(0), 
                    "New original field 'user_id' should be preserved");
        
        // Verify cache key is updated again
        assertNotNull(secondResponse.getFields().get("tf_cache_key"), 
                    "Second response should have a cache key");
        assertTrue(secondResponse.getFields().get("tf_cache_key").get(0).startsWith("tf_cache_"), 
                    "Second response cache key should start with 'tf_cache_'");
    }

    @Test
    public void testCacheMissScenario() {
        // Create input fields with cache key that doesn't exist in cache
        Map<String, String> inputFields = new HashMap<>();
        inputFields.put("tf_cache_key", "non-existent-key");
        inputFields.put("amount", "75.00");
        inputFields.put("currency", "GBP");
        inputFields.put("user_id", "user456");
        
        // Process transaction - should miss cache
        CiclopsResponse response = cachedTransactionRunner.processTransaction(inputFields, "test-trx-002");
        
        // Verify response contains only original fields (no hydration)
        assertNotNull(response, "Response should not be null");
        assertNotNull(response.getFields(), "Response fields should not be null");
        
        // Verify original fields are preserved
        assertEquals("user456", response.getFields().get("user_id").get(0), 
                    "Original field 'user_id' should be preserved");
        assertEquals("75.00", response.getFields().get("amount").get(0), 
                    "Amount should remain as original value");
        assertEquals("GBP", response.getFields().get("currency").get(0), 
                    "Currency should remain as original value");
        
        // Verify no cached fields are present
        assertNull(response.getFields().get("status"), 
                  "Status should not be present (cache miss)");
        assertNull(response.getFields().get("facility_id"), 
                  "Facility ID should not be present (cache miss)");
        
        // Verify total number of fields (only original fields)
        int expectedFieldCount = 4; // tf_cache_key, amount, currency, user_id
        assertEquals(expectedFieldCount, response.getFields().size(), 
                    "Should have " + expectedFieldCount + " fields (no hydration)");
    }

    @Test
    public void testNoCacheKeyScenario() {
        // Create input fields without cache key
        Map<String, String> inputFields = new HashMap<>();
        inputFields.put("amount", "25.00");
        inputFields.put("currency", "CAD");
        inputFields.put("user_id", "user789");
        
        // Process transaction - should proceed normally (no cache logic)
        CiclopsResponse response = cachedTransactionRunner.processTransaction(inputFields, "test-trx-003");
        
        // Verify response contains only original fields
        assertNotNull(response, "Response should not be null");
        assertNotNull(response.getFields(), "Response fields should not be null");
        
        // Verify original fields are preserved
        assertEquals("user789", response.getFields().get("user_id").get(0), 
                    "Original field 'user_id' should be preserved");
        assertEquals("25.00", response.getFields().get("amount").get(0), 
                    "Amount should remain as original value");
        assertEquals("CAD", response.getFields().get("currency").get(0), 
                    "Currency should remain as original value");
        
        // Verify no cache key field
        assertNull(response.getFields().get("tf_cache_key"), 
                  "Cache key should not be present");
        
        // Verify total number of fields (only original fields)
        int expectedFieldCount = 3; // amount, currency, user_id
        assertEquals(expectedFieldCount, response.getFields().size(), 
                    "Should have " + expectedFieldCount + " fields (no cache key)");
    }

    @Test
    public void testCacheKeyFieldPreservation() {
        // Populate cache first
        cachePopulator.populateCacheWithTestData();
        
        // Create input with cache key but also include some fields that exist in cache
        Map<String, String> inputFields = new HashMap<>();
        inputFields.put("tf_cache_key", "test-cache-key-123");
        inputFields.put("amount", "999.99"); // This should be overridden by cache
        inputFields.put("currency", "JPY"); // This should be overridden by cache
        inputFields.put("custom_field", "custom_value"); // This should be preserved
        inputFields.put("facility_id", "original_facility"); // This should be overridden by cache
        
        // Process transaction
        CiclopsResponse response = cachedTransactionRunner.processTransaction(inputFields, "test-trx-004");
        
        // Verify cache key is present (but it's been updated to a new generated key)
        assertNotNull(response.getFields().get("tf_cache_key"), 
                    "Cache key should be present");
        assertTrue(response.getFields().get("tf_cache_key").get(0).startsWith("tf_cache_"), 
                    "Cache key should start with 'tf_cache_'");
        
        // Verify cached values override original values
        assertEquals("100.50", response.getFields().get("amount").get(0), 
                    "Amount should be overridden by cache");
        assertEquals("USD", response.getFields().get("currency").get(0), 
                    "Currency should be overridden by cache");
        assertEquals("facility_001", response.getFields().get("facility_id").get(0), 
                    "Facility ID should be overridden by cache");
        
        // Verify non-conflicting fields are preserved
        assertEquals("custom_value", response.getFields().get("custom_field").get(0), 
                    "Custom field should be preserved");
        
        // Verify additional cached fields are present
        assertEquals("processed", response.getFields().get("status").get(0), 
                    "Status should be hydrated from cache");
    }
}
