package org.acme;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Named;
import jakarta.enterprise.inject.Default;
import java.util.Map;
import java.util.List;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Arrays;

@ApplicationScoped
@Default
@Named("testTransactionRunner")
public class TestTransactionRunner implements TransactionRunner {
    @Override
    public CiclopsResponse processTransaction(Map<String, String> fields, String trxId) {
        Map<String, List<String>> responseFields = new HashMap<>();
        
        // Count tablefacility fields from incoming fields (from cache)
        int tfFieldCount = 0;
        if (fields != null) {
            for (Map.Entry<String, String> entry : fields.entrySet()) {
                if (entry.getKey().toLowerCase().startsWith("tablefacility")) {
                    tfFieldCount++;
                }
            }
        }
        
        
        // Convert all fields to response format
        // DON'T ADD TF FIELDS FROM THE INPUT FILEDS! (FILTER THEM OUT!)
        if (fields != null) {
            for (Map.Entry<String, String> entry : fields.entrySet()) {
                // Filter out tablefacility fields from input
                if (!entry.getKey().toLowerCase().startsWith("tablefacility")) {
                    List<String> values = new ArrayList<>();
                    values.add(entry.getValue());
                    responseFields.put(entry.getKey(), values);
                }
            }
        }
        
        // Add debug field with count of TF fields found from cache
        if (tfFieldCount > 0) {
            responseFields.put("tf_input_found", Arrays.asList(String.valueOf(tfFieldCount)));
        }

        // Generate mock tablefacility fields if mockTF field is present
        if (fields != null && fields.containsKey("mockTF")) {
            try {
                int mockCount = Integer.parseInt(fields.get("mockTF"));
                // Generate mock tablefacility fields and add them to response
                for (int i = 1; i <= mockCount; i++) {
                    responseFields.put("tablefacility_" + i, Arrays.asList("mock_value_" + i));
                }
                // Add tf_input_found with count of generated mock fields
                responseFields.put("tf_input_found", Arrays.asList(String.valueOf(mockCount)));
            } catch (NumberFormatException e) {
                // If mockTF is not a valid number, default to 10
                for (int i = 1; i <= 10; i++) {
                    responseFields.put("tablefacility_" + i, Arrays.asList("mock_value_" + i));
                }              
            }
        }
        
        return new CiclopsResponse(responseFields);
    }
} 