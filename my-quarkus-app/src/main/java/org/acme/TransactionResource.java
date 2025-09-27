package org.acme;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import org.acme.dto.TransactionRequestDTO;
import org.eclipse.microprofile.config.inject.ConfigProperty;

@Path("/api")
public class TransactionResource {

    @Inject
    @Named("testTransactionRunner")
    TransactionRunner testRunner;
    
    @Inject
    @Named("cachedTransactionRunner")
    TransactionRunner wrappedRunner;
    
    @ConfigProperty(name = "app.transaction.wrapper.enabled", defaultValue = "false")
    boolean wrapperEnabled;

    @POST
    @Path("/transaction")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public CiclopsResponse processTransaction(TransactionRequestDTO request) {
        if (request == null || request.getFields() == null) {
            throw new IllegalArgumentException("Request or fields cannot be null");
        }
        
        // Simple conditional logic - much cleaner!
        TransactionRunner runner = wrapperEnabled ? wrappedRunner : testRunner;
        return runner.processTransaction(request.getFields(), request.getTrxId());
    }
} 