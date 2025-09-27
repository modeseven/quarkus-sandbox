package org.acme;

import java.util.Map;

public interface TransactionRunner {
    /**
     * Process a transaction with the given fields and transaction ID
     * @param fields The input fields for the transaction
     * @param trxId The transaction ID
     * @return A CiclopsResponse containing the processed fields
     */
    CiclopsResponse processTransaction(Map<String, String> fields, String trxId);
} 