package uk.co.hsim.assetaudit.importfile;

import java.util.Locale;

public final class AssetFileFormatDetector {
    private AssetFileFormatDetector() {
    }

    public static AssetFileFormat detect(String displayName, String mimeType) {
        String name = displayName == null ? "" : displayName.toLowerCase(Locale.ROOT);
        String type = mimeType == null ? "" : mimeType.toLowerCase(Locale.ROOT);
        if (name.endsWith(".xlsx") || type.equals("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")) {
            return AssetFileFormat.XLSX;
        }
        if (name.endsWith(".csv")
                || type.equals("text/csv")
                || type.equals("text/comma-separated-values")
                || type.equals("application/vnd.ms-excel")) {
            return AssetFileFormat.CSV;
        }
        return AssetFileFormat.UNSUPPORTED;
    }
}
