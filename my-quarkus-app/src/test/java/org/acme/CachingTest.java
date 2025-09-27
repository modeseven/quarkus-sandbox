package org.acme;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.Test;
import org.acme.dto.TransactionRequestDTO;

import java.util.HashMap;
import java.util.Map;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.is;

@QuarkusTest
public class CachingTest {

    @Test
    public void testTransactionWithCacheKey() {
        // Create a transaction request with a cache key
        Map<String, String> fields = new HashMap<>();
        fields.put("tf_cache_key", "test-cache-key-123");
        fields.put("amount", "100.50");
        fields.put("currency", "USD");
        fields.put("timestamp", String.valueOf(System.currentTimeMillis()));

        TransactionRequestDTO request = new TransactionRequestDTO();
        request.setFields(fields);
        request.setTrxId("test-trx-001");
        request.setInit(true);

        // First request - should be a cache miss and process normally
        given()
            .contentType(ContentType.JSON)
            .body(request)
            .when()
            .post("/api/transaction")
            .then()
            .statusCode(200)
            .body("fields.tf_cache_key[0]", is("test-cache-key-123"))
            .body("fields.amount[0]", is("100.50"))
            .body("fields.currency[0]", is("USD"));

        // Second request with same cache key - should be a cache hit
        given()
            .contentType(ContentType.JSON)
            .body(request)
            .when()
            .post("/api/transaction")
            .then()
            .statusCode(200)
            .body("fields.tf_cache_key[0]", is("test-cache-key-123"))
            .body("fields.amount[0]", is("100.50"))
            .body("fields.currency[0]", is("USD"));
    }

    @Test
    public void testTransactionWithoutCacheKey() {
        // Create a transaction request without a cache key
        Map<String, String> fields = new HashMap<>();
        fields.put("amount", "200.75");
        fields.put("currency", "EUR");
        fields.put("timestamp", String.valueOf(System.currentTimeMillis()));

        TransactionRequestDTO request = new TransactionRequestDTO();
        request.setFields(fields);
        request.setTrxId("test-trx-002");
        request.setInit(true);

        // Request without cache key - should process normally
        given()
            .contentType(ContentType.JSON)
            .body(request)
            .when()
            .post("/api/transaction")
            .then()
            .statusCode(200)
            .body("fields.amount[0]", is("200.75"))
            .body("fields.currency[0]", is("EUR"));
    }
}
