package org.acme;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import org.acme.cache.FieldHydrationService;
import org.acme.cache.ResponseCacheService;
import java.util.List;
import java.util.Map;

/**
 * Cached functional wrapper for TransactionRunner that integrates caching functionality
 * using Java 8+ functional interfaces with pre/post processing capabilities.
 */
@ApplicationScoped
@Named("cachedTransactionRunner")
public class CachedTransactionRunnerWrapper implements TransactionRunner {
    
    @Inject
    @Named("testTransactionRunner")
    TransactionRunner delegate;
    
    @Inject
    FieldHydrationService fieldHydrationService;
    
    @Inject
    ResponseCacheService responseCacheService;
    
    public CachedTransactionRunnerWrapper() {
        // No initialization needed - preprocessing is done inline
    }

    @Override
    public CiclopsResponse processTransaction(Map<String, String> fields, String trxId) {
        System.out.println("I'm here, preprocessing input");
        
        // Pre-process: Hydrate fields with cached data if available
        System.out.println("Pre-processing: Hydrating fields with cached data for transaction " + trxId);
        Map<String, String> hydratedFields = fieldHydrationService.hydrateFields(fields);
        
        // Delegate
        CiclopsResponse response = delegate.processTransaction(hydratedFields, trxId);
        
        System.out.println("After delegate, I'm here postprocessing output");
        
        // Post-process with the actual transaction ID
        System.out.println("Post-processing: Adding timestamp and processing response for caching");
        // Add timestamp to response
        CiclopsResponse enhancedResponse = new CiclopsResponse(response.getFields());
        enhancedResponse.addField("_postprocessing_timestamp", List.of(String.valueOf(System.currentTimeMillis())));
        // Process response to cache tablefacility fields if present
        return responseCacheService.processResponse(enhancedResponse, trxId);
    }

}
