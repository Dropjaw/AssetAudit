package uk.co.hsim.assetaudit.importfile;

public final class AssetFileMetadata {
    private final String auditName;
    private final String sourceFileName;
    private final AssetFileFormat format;
    private final int sourceRowCount;

    public AssetFileMetadata(String auditName, String sourceFileName, AssetFileFormat format, int sourceRowCount) {
        this.auditName = auditName == null ? "" : auditName;
        this.sourceFileName = sourceFileName == null ? "" : sourceFileName;
        this.format = format == null ? AssetFileFormat.UNSUPPORTED : format;
        this.sourceRowCount = sourceRowCount;
    }

    public String getAuditName() {
        return auditName;
    }

    public String getSourceFileName() {
        return sourceFileName;
    }

    public AssetFileFormat getFormat() {
        return format;
    }

    public int getSourceRowCount() {
        return sourceRowCount;
    }
}
