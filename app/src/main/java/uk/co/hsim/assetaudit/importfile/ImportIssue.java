package uk.co.hsim.assetaudit.importfile;

import uk.co.hsim.assetaudit.domain.enums.ImportIssueSeverity;

public final class ImportIssue {
    private final Integer rowNumber;
    private final String columnName;
    private final ImportIssueSeverity severity;
    private final ImportIssueCode code;
    private final String message;
    private final String sourceValue;

    public ImportIssue(Integer rowNumber, String columnName, ImportIssueSeverity severity,
                       ImportIssueCode code, String message, String sourceValue) {
        this.rowNumber = rowNumber;
        this.columnName = columnName;
        this.severity = severity;
        this.code = code;
        this.message = message;
        this.sourceValue = sourceValue;
    }

    public Integer getRowNumber() {
        return rowNumber;
    }

    public String getColumnName() {
        return columnName;
    }

    public ImportIssueSeverity getSeverity() {
        return severity;
    }

    public ImportIssueCode getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }

    public String getSourceValue() {
        return sourceValue;
    }
}
