package org.acme;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.acme.cache.FieldHydrationService;
import org.acme.cache.ResponseCacheService;
import org.acme.qualifiers.CachedTransactionRunnerQualifier;

import java.util.Map;

/**
 * Cached implementation of TransactionRunner that implements the caching specification.
 * This wrapper adds caching logic around the actual transaction processing.
 */
@ApplicationScoped
@CachedTransactionRunnerQualifier
public class CachedTransactionRunner implements TransactionRunner {

    @Inject
    TestTransactionRunner testTransactionRunner;

    @Inject
    FieldHydrationService fieldHydrationService;

    @Inject
    ResponseCacheService responseCacheService;

    @Override
    public CiclopsResponse processTransaction(Map<String, String> fields, String trxId) {
        // Hydrate fields with cached data if available
        Map<String, String> hydratedFields = fieldHydrationService.hydrateFields(fields);

        CiclopsResponse response = testTransactionRunner.processTransaction(hydratedFields, trxId);

        // Process response to cache tablefacility fields if present
        return responseCacheService.processResponse(response, trxId);
    }

}
