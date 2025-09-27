package org.acme;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Alternative;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Named;
import java.util.Map;
import java.util.List;
import java.util.ArrayList;
import java.util.HashMap;

/**
 * Alternative TestTransactionRunner that returns TF fields in responses.
 * This is used for testing the caching functionality.
 */
@ApplicationScoped
@Alternative
@Named("testTransactionRunner")
public class TestTransactionRunnerWithTfFields implements TransactionRunner {
    
    @Override
    public CiclopsResponse processTransaction(Map<String, String> fields, String trxId) {
        // Convert Map<String, String> to Map<String, List<String>>
        Map<String, List<String>> responseFields = new HashMap<>();
        
        if (fields != null) {
            for (Map.Entry<String, String> entry : fields.entrySet()) {
                List<String> values = new ArrayList<>();
                values.add(entry.getValue());
                responseFields.put(entry.getKey(), values);
            }
        }
        
        // Add 10 tablefacility fields to response (this is what gets cached)
        for (int i = 1; i <= 10; i++) {
            responseFields.put("tablefacility_" + i, List.of("cached_data_" + i));
        }
        
        // Add some other response fields
        responseFields.put("status", List.of("success"));
        responseFields.put("transaction_id", List.of(trxId));
        
        return new CiclopsResponse(responseFields);
    }
}
