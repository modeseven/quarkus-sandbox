package org.acme;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import org.acme.dto.TransactionRequestDTO;

@Path("/api")
public class TransactionResource {

    @POST
    @Path("/transaction")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public String processTransaction(TransactionRequestDTO request) {
        // TODO: Implement your transaction processing logic here
        return "Transaction processed for ID: " + request.getTrxId();
    }
} 