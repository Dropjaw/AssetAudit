package uk.co.hsim.assetaudit.export;

public final class ExportFileRecord {
    private final String fileName;
    private final String mediaType;
    private final int rowCount;
    private final String sha256;

    public ExportFileRecord(String fileName, String mediaType, int rowCount, String sha256) {
        this.fileName = fileName;
        this.mediaType = mediaType;
        this.rowCount = rowCount;
        this.sha256 = sha256;
    }

    public String getFileName() {
        return fileName;
    }

    public String getMediaType() {
        return mediaType;
    }

    public int getRowCount() {
        return rowCount;
    }

    public String getSha256() {
        return sha256;
    }
}
