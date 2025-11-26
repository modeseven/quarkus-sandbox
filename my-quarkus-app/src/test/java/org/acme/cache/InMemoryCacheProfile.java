package org.acme.cache;

import io.quarkus.test.junit.QuarkusTestProfile;

import java.util.Map;

/**
 * Profile 2: Caching enabled with in-memory type
 */
public class InMemoryCacheProfile implements QuarkusTestProfile {
    @Override
    public Map<String, String> getConfigOverrides() {
        return Map.of(
            "app.caching.enabled", "true",
            "app.caching.type", "in-memory"
        );
    }
}

