package org.acme;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import org.acme.dto.TransactionRequestDTO;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

/**
 * Simple integration test for TransactionResource that tests both caching scenarios:
 * 1. Regular fields (no cache key generated)
 * 2. Tablefacility fields (cache key generated)
 * 
 * This test uses the actual running service and doesn't require Redis.
 */
@QuarkusTest
public class TransactionResourceSimpleTest {

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
        fields.put("mockTF", "5"); // This triggers generation of 5 mock tablefacility fields
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
            .body("fields.tf_cache_key[0]", startsWith("TF_CACHE_")) // Cache key format
            .body("fields.tf_input_found", hasItem("5")); // Should have tf_input_found with count 5
    }

    @Test
    void testCacheKeyResubmission_ShouldProcessWithCacheKey() {
        // First, create a cache entry by processing a mockTF request
        TransactionRequestDTO initialRequest = new TransactionRequestDTO();
        Map<String, String> initialFields = new HashMap<>();
        initialFields.put("mockTF", "3"); // This triggers generation of 3 mock tablefacility fields
        initialFields.put("status", "pending");
        initialFields.put("amount", "100.00");
        initialRequest.setFields(initialFields);
        initialRequest.setTrxId("cache_test_1");

        // Process initial request to create cache entry
        String cacheKey = given()
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

        // Verify we got a cache key
        assert cacheKey != null;
        assert cacheKey.startsWith("TF_CACHE_");

        // Now test cache hydration by resubmitting with the cache key
        TransactionRequestDTO hydrationRequest = new TransactionRequestDTO();
        Map<String, String> hydrationFields = new HashMap<>();
        hydrationFields.put("tf_cache_key", cacheKey); // Use the cache key from previous response
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
            .body("fields.tf_input_found", hasItem("3")); // Should detect 3 TF fields from cache
    }

    @Test
    void testEndToEndCacheFlow() {
        // Complete end-to-end test of the cache flow
        
        // Step 1: Process mockTF request (should cache and return cache key)
        TransactionRequestDTO step1Request = new TransactionRequestDTO();
        Map<String, String> step1Fields = new HashMap<>();
        step1Fields.put("mockTF", "4"); // This triggers generation of 4 mock tablefacility fields
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

        assert cacheKey != null;
        assert cacheKey.startsWith("TF_CACHE_");

        // Step 2: Use cache key to hydrate new request
        TransactionRequestDTO step2Request = new TransactionRequestDTO();
        Map<String, String> step2Fields = new HashMap<>();
        step2Fields.put("tf_cache_key", cacheKey);
        step2Fields.put("new_transaction_field", "new_value");
        step2Request.setFields(step2Fields);
        step2Request.setTrxId("e2e_test_2");

        given()
            .contentType(ContentType.JSON)
            .body(step2Request)
        .when()
            .post("/api/transaction")
        .then()
            .statusCode(200)
            .body("fields.new_transaction_field", hasItem("new_value"))
            .body("fields._postprocessing_timestamp", notNullValue())
            .body("fields.tf_input_found", hasItem("4")); // Should detect 4 TF fields from cache
    }

    @Test
    void testRegularFields_ShouldNotHaveTfInputFound() {
        // Test that regular fields without TF fields don't generate tf_input_found
        TransactionRequestDTO request = new TransactionRequestDTO();
        Map<String, String> fields = new HashMap<>();
        fields.put("hello", "world");
        fields.put("status", "active");
        request.setFields(fields);
        request.setTrxId("test_no_tf");

        given()
            .contentType(ContentType.JSON)
            .body(request)
        .when()
            .post("/api/transaction")
        .then()
            .statusCode(200)
            .body("fields.hello", hasItem("world"))
            .body("fields.status", hasItem("active"))
            .body("fields._postprocessing_timestamp", notNullValue())
            .body("fields.tf_input_found", nullValue()) // Should NOT have tf_input_found
            .body("fields.tf_cache_key", nullValue()); // Should NOT have cache key
    }

}
