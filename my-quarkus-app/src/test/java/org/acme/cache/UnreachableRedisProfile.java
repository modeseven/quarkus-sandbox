package org.acme.cache;

import io.quarkus.test.junit.QuarkusTestProfile;

import java.util.Map;

/**
 * Profile 3: Caching enabled with redis but unreachable host
 */
public class UnreachableRedisProfile implements QuarkusTestProfile {
    @Override
    public Map<String, String> getConfigOverrides() {
        return Map.of(
            "app.caching.enabled", "true",
            "app.caching.type", "redis",
            "quarkus.redis.hosts", "redis://unreachable-host:6379"
        );
    }
}

