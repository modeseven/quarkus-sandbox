package org.acme.dto;

import java.util.Map;

public class TransactionRequestDTO {
    private Map<String, String> fields;
    private String trxId;
    private Boolean init;

    // Getters and Setters
    public Map<String, String> getFields() {
        return fields;
    }

    public void setFields(Map<String, String> fields) {
        this.fields = fields;
    }

    public String getTrxId() {
        return trxId;
    }

    public void setTrxId(String trxId) {
        this.trxId = trxId;
    }

    public Boolean getInit() {
        return init;
    }

    public void setInit(Boolean init) {
        this.init = init;
    }
} 