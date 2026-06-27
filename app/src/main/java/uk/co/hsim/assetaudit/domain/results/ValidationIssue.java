package uk.co.hsim.assetaudit.domain.results;

import uk.co.hsim.assetaudit.domain.enums.ImportIssueSeverity;

public final class ValidationIssue {
    private final String fieldName;
    private final ImportIssueSeverity severity;
    private final String code;
    private final String message;

    public ValidationIssue(String fieldName, ImportIssueSeverity severity, String code, String message) {
        this.fieldName = fieldName;
        this.severity = severity;
        this.code = code;
        this.message = message;
    }

    public String getFieldName() {
        return fieldName;
    }

    public ImportIssueSeverity getSeverity() {
        return severity;
    }

    public String getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }
}
