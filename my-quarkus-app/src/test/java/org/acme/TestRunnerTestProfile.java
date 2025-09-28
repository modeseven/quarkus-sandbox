package org.acme;

import io.quarkus.test.junit.QuarkusTestProfile;

import java.util.Map;

/**
 * Test profile for testing with wrapper disabled (test runner)
 */
public class TestRunnerTestProfile implements QuarkusTestProfile {
    @Override
    public Map<String, String> getConfigOverrides() {
        return Map.of(
            "app.transaction.wrapper.enabled", "false"
        );
    }
}
