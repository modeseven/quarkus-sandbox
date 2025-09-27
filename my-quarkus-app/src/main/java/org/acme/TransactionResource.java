package org.acme;

import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import org.acme.dto.TransactionRequestDTO;
import org.acme.qualifiers.CachedTransactionRunnerQualifier;

@Path("/api")
public class TransactionResource {

    @Inject
    @CachedTransactionRunnerQualifier
    TransactionRunner transactionRunner;

    @POST
    @Path("/transaction")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public CiclopsResponse processTransaction(TransactionRequestDTO request) {
        if (request == null || request.getFields() == null) {
            throw new IllegalArgumentException("Request or fields cannot be null");
        }
        return transactionRunner.processTransaction(request.getFields(), request.getTrxId());
    }
} 