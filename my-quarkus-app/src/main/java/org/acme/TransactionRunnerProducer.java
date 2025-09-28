package org.acme;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

/**
 * Producer for TransactionRunner that conditionally provides either the cached wrapper
 * or the test implementation based on configuration.
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

    @ConfigProperty(name = "app.transaction.wrapper.enabled", defaultValue = "false")
    boolean wrapperEnabled;

    /**
     * Produces the appropriate TransactionRunner implementation based on configuration.
     * 
     * @return The configured transaction runner implementation
     */
    @Produces
    @ApplicationScoped
    @Named("selectedTransactionRunner")
    public TransactionRunner produceTransactionRunner() {
        LOG.infof("Configuring transaction runner with wrapper enabled: %s", wrapperEnabled);
        
        if (wrapperEnabled) {
            LOG.info("Using cached transaction runner wrapper");
            return wrappedRunner;
        } else {
            LOG.info("Using test transaction runner");
            return testRunner;
        }
    }
}
