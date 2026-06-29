package uk.co.hsim.assetaudit.export;

public final class ExportCompletionResult {
    private final String exportRunId;
    private final String message;

    public ExportCompletionResult(String exportRunId, String message) {
        this.exportRunId = exportRunId;
        this.message = message == null ? "" : message;
    }

    public String getExportRunId() {
        return exportRunId;
    }

    public String getMessage() {
        return message;
    }
}
