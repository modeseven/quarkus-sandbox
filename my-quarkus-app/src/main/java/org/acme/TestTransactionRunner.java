package org.acme;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Named;
import java.util.Map;
import java.util.List;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Arrays;

@ApplicationScoped
@Named("testTransactionRunner")
public class TestTransactionRunner implements TransactionRunner {
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
        
        // Add some tablefacility fields to simulate a real response
        responseFields.put("tablefacility_1", Arrays.asList("tf_data_1"));
        responseFields.put("tablefacility_2", Arrays.asList("tf_data_2"));
        responseFields.put("tablefacility_3", Arrays.asList("tf_data_3"));
        
        return new CiclopsResponse(responseFields);
    }
} 