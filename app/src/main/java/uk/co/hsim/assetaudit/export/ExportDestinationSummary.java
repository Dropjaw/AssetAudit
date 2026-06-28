package uk.co.hsim.assetaudit.export;

public final class ExportDestinationSummary {
    private final String displayName;
    private final String diagnosticSummary;

    public ExportDestinationSummary(String displayName, String diagnosticSummary) {
        this.displayName = displayName == null ? "" : displayName;
        this.diagnosticSummary = diagnosticSummary == null ? "" : diagnosticSummary;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getDiagnosticSummary() {
        return diagnosticSummary;
    }
}
