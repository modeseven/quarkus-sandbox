package org.acme;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import org.acme.config.CachingConfiguration;
import org.jboss.logging.Logger;

/**
 * Producer for TransactionRunner that conditionally provides either the cached wrapper
 * or the test implementation based on caching configuration.
 * 
 * Logic: If caching is enabled, use the wrapper; otherwise use the direct test runner.
 */
@ApplicationScoped
public class TransactionRunnerProducer {

    private static final Logger LOG = Logger.getLogger(TransactionRunnerProducer.class);

    @Inject
    @Named("testTransactionRunner")
    TransactionRunner testRunner;
    
    @Inject
    @Named("cachedTransactionRunner")
    TransactionRunner wrappedRunner;

    @Inject
    CachingConfiguration cachingConfiguration;

    /**
     * Produces the appropriate TransactionRunner implementation based on caching configuration.
     * 
     * @return The configured transaction runner implementation
     */
    @Produces
    @ApplicationScoped
    @Named("selectedTransactionRunner")
    public TransactionRunner produceTransactionRunner() {
        boolean cachingEnabled = cachingConfiguration.isCachingEnabled();
        LOG.infof("Configuring transaction runner with caching enabled: %s", cachingEnabled);
        
        if (cachingEnabled) {
            LOG.info("Caching enabled - using cached transaction runner wrapper");
            return wrappedRunner;
        } else {
            LOG.info("Caching disabled - using direct test transaction runner");
            return testRunner;
        }
    }
}
