package org.acme.util;

public class FieldConfig {
    private String fieldName;
    private int startIndex;
    private int lineNumber; // Line number, defaults to 1

    public FieldConfig() {
        this.lineNumber = 1; // Default to line 1
    }

    public FieldConfig(String fieldName, int startIndex) {
        this.fieldName = fieldName;
        this.startIndex = startIndex;
        this.lineNumber = 1; // Default to line 1
    }

    public FieldConfig(String fieldName, int startIndex, int lineNumber) {
        this.fieldName = fieldName;
        this.startIndex = startIndex;
        this.lineNumber = lineNumber;
    }

    public String getFieldName() {
        return fieldName;
    }

    public void setFieldName(String fieldName) {
        this.fieldName = fieldName;
    }

    public int getStartIndex() {
        return startIndex;
    }

    public void setStartIndex(int startIndex) {
        this.startIndex = startIndex;
    }

    public int getLineNumber() {
        return lineNumber;
    }

    public void setLineNumber(int lineNumber) {
        this.lineNumber = lineNumber;
    }
}
