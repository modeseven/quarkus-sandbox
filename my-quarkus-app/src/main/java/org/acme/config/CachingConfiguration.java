package org.acme.config;

import jakarta.enterprise.context.ApplicationScoped;
import org.eclipse.microprofile.config.inject.ConfigProperty;

/**
 * Configuration service for caching settings
 */
@ApplicationScoped
public class CachingConfiguration {

    @ConfigProperty(name = "app.caching.enabled", defaultValue = "false")
    boolean cachingEnabled;

    @ConfigProperty(name = "app.caching.tablefacility.ttl", defaultValue = "3600")
    int tablefacilityTtlSeconds;

    public boolean isCachingEnabled() {
        return cachingEnabled;
    }

    public int getTablefacilityTtlSeconds() {
        return tablefacilityTtlSeconds;
    }
}
