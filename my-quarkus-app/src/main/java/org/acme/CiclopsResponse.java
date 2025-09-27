package org.acme;

import java.util.Map;
import java.util.List;
import java.util.HashMap;

public class CiclopsResponse {
    private Map<String, List<String>> fields;

    public CiclopsResponse() {
        this.fields = new HashMap<>();
    }

    public CiclopsResponse(Map<String, List<String>> fields) {
        this.fields = fields != null ? new HashMap<>(fields) : new HashMap<>();
    }

    public Map<String, List<String>> getFields() {
        return fields;
    }

    public void setFields(Map<String, List<String>> fields) {
        this.fields = fields != null ? new HashMap<>(fields) : new HashMap<>();
    }

    public void addField(String key, List<String> values) {
        this.fields.put(key, values);
    }

    public List<String> getField(String key) {
        return this.fields.get(key);
    }
}
