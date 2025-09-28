package org.acme;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;
import io.restassured.http.ContentType;
import org.acme.dto.TransactionRequestDTO;
import org.junit.jupiter.api.Test;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import java.util.HashMap;
import java.util.Map;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Test for TransactionResource using the test runner (wrapper disabled).
 * This tests the scenario where app.transaction.wrapper.enabled=false
 */
@QuarkusTest
@TestProfile(TestRunnerTestProfile.class)
public class TransactionResourceTestRunnerTest {

    @Inject
    @Named("selectedTransactionRunner")
    TransactionRunner selectedRunner;

    @Inject
    @Named("testTransactionRunner")
    TransactionRunner testRunner;

    @Inject
    @Named("cachedTransactionRunner")
    TransactionRunner cachedRunner;

    @Test
    void testSelectedRunner_ShouldBeTestRunner() {
        // Verify that the DI-selected runner is the test runner when wrapper is disabled
        assertNotNull(selectedRunner, "Selected runner should not be null");
        // Test the behavior: test runner should NOT add postprocessing timestamp
        Map<String, String> testFields = Map.of("test", "value");
        CiclopsResponse response = selectedRunner.processTransaction(testFields, "test123");
        assertNotNull(response, "Response should not be null");
        assertFalse(response.getFields().containsKey("_postprocessing_timestamp"), 
            "Test runner should NOT add _postprocessing_timestamp field");
    }

    @Test
    void testRegularFields_ShouldNotHavePostprocessingTimestamp() {
        // Test with regular fields - should NOT have _postprocessing_timestamp (test runner doesn't add it)
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
            .body("fields._postprocessing_timestamp", nullValue()) // Should NOT have timestamp
            .body("fields.tf_cache_key", nullValue()); // Should NOT have cache key
    }

    @Test
    void testMockTFFields_ShouldGenerateMockFields_NoPostprocessing() {
        // Test with mockTF field - should generate mock fields but NO postprocessing timestamp
        TransactionRequestDTO request = new TransactionRequestDTO();
        Map<String, String> fields = new HashMap<>();
        fields.put("mockTF", "3"); // This triggers generation of 3 mock tablefacility fields
        request.setFields(fields);
        request.setTrxId("test456");

        given()
            .contentType(ContentType.JSON)
            .body(request)
        .when()
            .post("/api/transaction")
        .then()
            .statusCode(200)
            .body("fields._postprocessing_timestamp", nullValue()) // Should NOT have timestamp
            .body("fields.tf_cache_key", nullValue()) // Should NOT have cache key
            .body("fields.tf_input_found", hasItem("3")) // Should have tf_input_found with count 3
            .body("fields.tablefacility_1", hasItem("mock_value_1")) // Should have mock fields
            .body("fields.tablefacility_2", hasItem("mock_value_2"))
            .body("fields.tablefacility_3", hasItem("mock_value_3"));
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
            .body("fields._postprocessing_timestamp", nullValue()) // Should NOT have timestamp
            .body("fields.tf_input_found", nullValue()) // Should NOT have tf_input_found
            .body("fields.tf_cache_key", nullValue()); // Should NOT have cache key
    }
}

