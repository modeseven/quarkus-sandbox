package org.acme.cache;

import io.quarkus.test.junit.QuarkusTestProfile;

import java.util.Map;

/**
 * Profile 1: No caching properties defined (or explicitly disabled)
 * Note: Since test application.properties has defaults, we explicitly disable
 */
public class NoCachingPropsProfile implements QuarkusTestProfile {
    @Override
    public Map<String, String> getConfigOverrides() {
        // Explicitly disable caching - simulates no caching props
        return Map.of(
            "app.caching.enabled", "false"
            // Don't set app.caching.type - let it default (won't matter since enabled=false)
        );
    }
}

