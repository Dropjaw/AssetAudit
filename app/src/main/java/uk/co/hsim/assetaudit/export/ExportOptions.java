package uk.co.hsim.assetaudit.export;

public final class ExportOptions {
    private final ExportMode exportMode;
    private final boolean excelFriendlyCsv;
    private final boolean includeReadme;

    public ExportOptions(ExportMode exportMode, boolean excelFriendlyCsv, boolean includeReadme) {
        this.exportMode = exportMode;
        this.excelFriendlyCsv = excelFriendlyCsv;
        this.includeReadme = includeReadme;
    }

    public static ExportOptions defaults(ExportMode exportMode) {
        return new ExportOptions(exportMode, true, true);
    }

    public ExportMode getExportMode() {
        return exportMode;
    }

    public boolean isExcelFriendlyCsv() {
        return excelFriendlyCsv;
    }

    public boolean isIncludeReadme() {
        return includeReadme;
    }
}
