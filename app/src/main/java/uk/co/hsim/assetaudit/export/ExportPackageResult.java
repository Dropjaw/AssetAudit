package uk.co.hsim.assetaudit.export;

import java.util.List;

public final class ExportPackageResult {
    private final String packageId;
    private final String manifestJson;
    private final List<ExportFileRecord> files;

    public ExportPackageResult(String packageId, String manifestJson, List<ExportFileRecord> files) {
        this.packageId = packageId;
        this.manifestJson = manifestJson;
        this.files = files;
    }

    public String getPackageId() { return packageId; }
    public String getManifestJson() { return manifestJson; }
    public List<ExportFileRecord> getFiles() { return files; }
}
