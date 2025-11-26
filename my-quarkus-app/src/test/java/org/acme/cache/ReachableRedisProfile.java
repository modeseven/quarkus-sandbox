package org.acme.cache;

import io.quarkus.test.junit.QuarkusTestProfile;

import java.util.Map;

/**
 * Profile 4: Caching enabled with redis and reachable host
 */
public class ReachableRedisProfile implements QuarkusTestProfile {
    @Override
    public Map<String, String> getConfigOverrides() {
        return Map.of(
            "app.caching.enabled", "true",
            "app.caching.type", "redis",
            "quarkus.redis.hosts", "redis://localhost:6379"
        );
    }
}

