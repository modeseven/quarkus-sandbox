package org.acme;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import org.acme.cache.CacheService;
import org.acme.cache.FieldHydrationService;
import org.acme.cache.ResponseCacheService;
import org.acme.cache.qualifiers.DefaultCacheImpl;
import org.acme.config.CachingConfiguration;
import org.acme.dto.TransactionRequestDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import java.util.HashMap;
import java.util.Map;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration test for TransactionResource that tests both caching scenarios:
 * 1. Regular fields (no cache key generated)
 * 2. Tablefacility fields (cache key generated)
 * 3. Cache hydration functionality
 */
@QuarkusTest
public class TransactionResourceIntegrationTest {

    @Inject
    @Named("selectedTransactionRunner")
    TransactionRunner selectedRunner;

    @Inject
    @Named("cachedTransactionRunner")
    TransactionRunner cachedRunner;

    @Inject
    @DefaultCacheImpl
    CacheService cacheService;

    @Inject
    FieldHydrationService fieldHydrationService;

    @Inject
    ResponseCacheService responseCacheService;

    @Inject
    CachingConfiguration cachingConfiguration;

    @BeforeEach
    void setUp() {
        // Clear cache before each test
        // Note: Cache clearing depends on the cache implementation
        // For in-memory cache, we'll rely on test isolation
    }

    @Test
    void testSelectedRunner_ShouldBeCachedRunner() {
        // Verify that the DI-selected runner is the cached runner
        assertNotNull(selectedRunner, "Selected runner should not be null");
        // Test the behavior: cached runner should add postprocessing timestamp
        Map<String, String> testFields = Map.of("test", "value");
        CiclopsResponse response = selectedRunner.processTransaction(testFields, "test123");
        assertNotNull(response, "Response should not be null");
        assertTrue(response.getFields().containsKey("_postprocessing_timestamp"), 
            "Cached runner should add _postprocessing_timestamp field");
    }

    @Test
    void testRegularFields_ShouldNotGenerateCacheKey() {
        // Test with regular fields that should NOT generate a cache key
        TransactionRequestDTO request = new TransactionRequestDTO();
        Map<String, String> fields = new HashMap<>();
        fields.put("hello", "world");
        request.setFields(fields);
        request.setTrxId("test123");

        given()
            .contentType(ContentType.JSON)
            .body(request)
        .when()
            .post("/api/transaction")
        .then()
            .statusCode(200)
            .body("fields.hello", hasItem("world"))
            .body("fields._postprocessing_timestamp", notNullValue())
            .body("fields.tf_cache_key", nullValue()); // Should NOT have cache key
    }

    @Test
    void testTablefacilityFields_ShouldGenerateCacheKey() {
        // Test with mockTF field that SHOULD generate mock tablefacility fields and cache key
        TransactionRequestDTO request = new TransactionRequestDTO();
        Map<String, String> fields = new HashMap<>();
        fields.put("mockTF", "7"); // This triggers generation of 7 mock tablefacility fields
        request.setFields(fields);
        request.setTrxId("test456");

        given()
            .contentType(ContentType.JSON)
            .body(request)
        .when()
            .post("/api/transaction")
        .then()
            .statusCode(200)
            .body("fields._postprocessing_timestamp", notNullValue())
            .body("fields.tf_cache_key", notNullValue()) // Should have cache key
            .body("fields.tf_cache_key[0]", startsWith("tf_cache_")) // Cache key format
            .body("fields.tf_input_found", hasItem("7")); // Should have tf_input_found with count 7
    }

    @Test
    void testCacheHydration_WithCacheKey_ShouldHydrateFields() {
        // First, create a cache entry by processing a mockTF request
        TransactionRequestDTO initialRequest = new TransactionRequestDTO();
        Map<String, String> initialFields = new HashMap<>();
        initialFields.put("mockTF", "6"); // This triggers generation of 6 mock tablefacility fields
        initialFields.put("status", "pending");
        initialFields.put("amount", "100.00");
        initialRequest.setFields(initialFields);
        initialRequest.setTrxId("cache_test_1");

        // Process initial request to create cache entry
        String initialResponse = given()
            .contentType(ContentType.JSON)
            .body(initialRequest)
        .when()
            .post("/api/transaction")
        .then()
            .statusCode(200)
            .extract()
            .body()
            .jsonPath()
            .getString("fields.tf_cache_key[0]");

        assertNotNull(initialResponse, "Initial request should generate a cache key");
        assertTrue(initialResponse.startsWith("tf_cache_"), "Cache key should have correct prefix");

        // Verify cache entry was created
        System.out.println("Looking for cache key: " + initialResponse);
        String cachedData = cacheService.get(initialResponse);
        System.out.println("Cache data found: " + (cachedData != null ? "YES" : "NO"));
        if (cachedData != null) {
            System.out.println("Cache data: " + cachedData);
        }
        assertNotNull(cachedData, "Cache entry should exist");
        assertTrue(cachedData.contains("tablefacility_1"), "Cached data should contain mock tablefacility field");
        // Note: Only tablefacility fields are cached, not other fields like status/amount

        // Now test cache hydration by resubmitting with the cache key
        TransactionRequestDTO hydrationRequest = new TransactionRequestDTO();
        Map<String, String> hydrationFields = new HashMap<>();
        hydrationFields.put("tf_cache_key", initialResponse); // Use the cache key from previous response
        hydrationFields.put("new_field", "new_value"); // Add a new field
        hydrationRequest.setFields(hydrationFields);
        hydrationRequest.setTrxId("cache_test_2");

        // Process hydration request - this should trigger cache hydration
        given()
            .contentType(ContentType.JSON)
            .body(hydrationRequest)
        .when()
            .post("/api/transaction")
        .then()
            .statusCode(200)
            .body("fields.new_field", hasItem("new_value")) // Original field should be preserved
            .body("fields._postprocessing_timestamp", notNullValue())
            .body("fields.tf_input_found", hasItem("6")); // Should detect 6 TF fields from cache
    }

    @Test
    void testCacheHydration_WithoutCacheKey_ShouldNotHydrate() {
        // Test that fields without cache key are not hydrated
        TransactionRequestDTO request = new TransactionRequestDTO();
        Map<String, String> fields = new HashMap<>();
        fields.put("regular_field", "regular_value");
        request.setFields(fields);
        request.setTrxId("no_cache_test");

        given()
            .contentType(ContentType.JSON)
            .body(request)
        .when()
            .post("/api/transaction")
        .then()
            .statusCode(200)
            .body("fields.regular_field", hasItem("regular_value"))
            .body("fields._postprocessing_timestamp", notNullValue())
            .body("fields.tf_cache_key", nullValue()); // Should NOT have cache key
    }

    @Test
    void testCacheHydration_WithInvalidCacheKey_ShouldNotHydrate() {
        // Test with invalid cache key
        TransactionRequestDTO request = new TransactionRequestDTO();
        Map<String, String> fields = new HashMap<>();
        fields.put("tf_cache_key", "invalid_cache_key_12345");
        fields.put("regular_field", "regular_value");
        request.setFields(fields);
        request.setTrxId("invalid_cache_test");

        given()
            .contentType(ContentType.JSON)
            .body(request)
        .when()
            .post("/api/transaction")
        .then()
            .statusCode(200)
            .body("fields.regular_field", hasItem("regular_value"))
            .body("fields._postprocessing_timestamp", notNullValue());
    }

    @Test
    void testEndToEndCacheFlow() {
        // Complete end-to-end test of the cache flow
        
        // Step 1: Process mockTF request (should cache and return cache key)
        TransactionRequestDTO step1Request = new TransactionRequestDTO();
        Map<String, String> step1Fields = new HashMap<>();
        step1Fields.put("mockTF", "8"); // This triggers generation of 8 mock tablefacility fields
        step1Fields.put("status", "active");
        step1Fields.put("amount", "250.00");
        step1Request.setFields(step1Fields);
        step1Request.setTrxId("e2e_test_1");

        String cacheKey = given()
            .contentType(ContentType.JSON)
            .body(step1Request)
        .when()
            .post("/api/transaction")
        .then()
            .statusCode(200)
            .body("fields.tf_cache_key", notNullValue())
            .extract()
            .body()
            .jsonPath()
            .getString("fields.tf_cache_key[0]");

        assertNotNull(cacheKey, "Step 1 should generate cache key");

        // Step 2: Verify cache entry exists
        String cachedData = cacheService.get(cacheKey);
        assertNotNull(cachedData, "Cache entry should exist after step 1");
        assertTrue(cachedData.contains("tablefacility_1"), "Cache should contain mock tablefacility data");
        // Note: Only tablefacility fields are cached, not other fields like status/amount

        // Step 3: Use cache key to hydrate new request
        TransactionRequestDTO step3Request = new TransactionRequestDTO();
        Map<String, String> step3Fields = new HashMap<>();
        step3Fields.put("tf_cache_key", cacheKey);
        step3Fields.put("new_transaction_field", "new_value");
        step3Request.setFields(step3Fields);
        step3Request.setTrxId("e2e_test_2");

        given()
            .contentType(ContentType.JSON)
            .body(step3Request)
        .when()
            .post("/api/transaction")
        .then()
            .statusCode(200)
            .body("fields.new_transaction_field", hasItem("new_value"))
            .body("fields._postprocessing_timestamp", notNullValue())
            .body("fields.tf_input_found", hasItem("8")); // Should detect 8 TF fields from cache

        // Step 4: Verify cache was accessed for hydration
        // Note: In a real test, we would verify that the cache was accessed
        // This would require additional test infrastructure to track cache calls
    }
}
