package org.acme;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test for TransactionRunnerProducer to verify it correctly selects the appropriate runner
 * based on configuration.
 */
@QuarkusTest
public class TransactionRunnerProducerTest {

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
    void testSelectedRunner_ShouldBeCachedRunner() {
        // With app.caching.enabled=true, should return cached runner
        assertNotNull(selectedRunner, "Selected runner should not be null");
        // Test the behavior: cached runner should add postprocessing timestamp
        Map<String, String> testFields = Map.of("test", "value");
        CiclopsResponse response = selectedRunner.processTransaction(testFields, "test123");
        assertNotNull(response, "Response should not be null");
        assertTrue(response.getFields().containsKey("_postprocessing_timestamp"), 
            "Cached runner should add _postprocessing_timestamp field");
    }

    @Test
    void testSelectedRunner_ShouldImplementTransactionRunner() {
        assertNotNull(selectedRunner, "Selected runner should not be null");
        assertTrue(selectedRunner instanceof TransactionRunner, "Selected runner should implement TransactionRunner interface");
    }
}


/**
 * Test for TransactionRunnerProducer with caching disabled
 */
@QuarkusTest
@TestProfile(DisabledWrapperTestProfile.class)
class TransactionRunnerProducerDisabledCachingTest {

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
        // With app.caching.enabled=false, should return test runner
        assertNotNull(selectedRunner, "Selected runner should not be null");
        // Test the behavior: test runner should NOT add postprocessing timestamp
        Map<String, String> testFields = Map.of("test", "value");
        CiclopsResponse response = selectedRunner.processTransaction(testFields, "test123");
        assertNotNull(response, "Response should not be null");
        assertFalse(response.getFields().containsKey("_postprocessing_timestamp"), 
            "Test runner should NOT add _postprocessing_timestamp field");
    }
}
