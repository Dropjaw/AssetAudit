package uk.co.hsim.assetaudit.importfile;

import android.net.Uri;

public final class DocumentReference {
    private final Uri uri;
    private final String displayName;
    private final String mimeType;
    private final long sizeBytes;
    private final AssetFileFormat detectedFormat;

    public DocumentReference(Uri uri, String displayName, String mimeType, long sizeBytes,
                             AssetFileFormat detectedFormat) {
        this.uri = uri;
        this.displayName = displayName == null ? "" : displayName;
        this.mimeType = mimeType == null ? "" : mimeType;
        this.sizeBytes = sizeBytes;
        this.detectedFormat = detectedFormat == null ? AssetFileFormat.UNSUPPORTED : detectedFormat;
    }

    public Uri getUri() {
        return uri;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getMimeType() {
        return mimeType;
    }

    public long getSizeBytes() {
        return sizeBytes;
    }

    public AssetFileFormat getDetectedFormat() {
        return detectedFormat;
    }
}
