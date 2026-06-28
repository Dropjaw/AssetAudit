package uk.co.hsim.assetaudit.export;

import android.content.ContentResolver;
import android.net.Uri;

import java.io.OutputStream;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import uk.co.hsim.assetaudit.domain.results.ErrorCode;
import uk.co.hsim.assetaudit.domain.results.OperationResult;
import uk.co.hsim.assetaudit.util.device.DeviceInfoProvider;

public final class ExportPackageService {
    private final CsvReportWriter csvWriter;
    private final JsonManifestWriter manifestWriter;
    private final DeviceInfoProvider deviceInfoProvider;

    public ExportPackageService(CsvReportWriter csvWriter, JsonManifestWriter manifestWriter,
                                DeviceInfoProvider deviceInfoProvider) {
        this.csvWriter = csvWriter;
        this.manifestWriter = manifestWriter;
        this.deviceInfoProvider = deviceInfoProvider;
    }

    public OperationResult<ExportPackageResult> writePackage(ContentResolver resolver, Uri uri,
                                                            ExportSnapshot snapshot, ExportOptions options) {
        try {
            List<EntryBytes> entries = buildEntries(snapshot, options);
            List<ExportFileRecord> records = new ArrayList<>();
            for (EntryBytes entry : entries) {
                records.add(new ExportFileRecord(entry.name, entry.mediaType, entry.rowCount, sha256(entry.bytes)));
            }
            String manifest = manifestWriter.write(snapshot, options, records, deviceInfoProvider);
            byte[] manifestBytes = manifest.getBytes(java.nio.charset.StandardCharsets.UTF_8);
            records.add(new ExportFileRecord("export_manifest.json", "application/json", 1, sha256(manifestBytes)));
            entries.add(new EntryBytes("export_manifest.json", "application/json", 1, manifestBytes));
            OutputStream output = resolver.openOutputStream(uri, "w");
            if (output == null) {
                return OperationResult.fail(ErrorCode.DATABASE_ERROR, "Unable to open export destination.");
            }
            try (ZipOutputStream zip = new ZipOutputStream(output)) {
                for (EntryBytes entry : entries) {
                    zip.putNextEntry(new ZipEntry(entry.name));
                    zip.write(entry.bytes);
                    zip.closeEntry();
                }
            }
            return OperationResult.ok(new ExportPackageResult(snapshot.packageId, manifest, records));
        } catch (Exception e) {
            return OperationResult.fail(ErrorCode.DATABASE_ERROR, "Export package could not be written.");
        }
    }

    private List<EntryBytes> buildEntries(ExportSnapshot snapshot, ExportOptions options) {
        ExportReportBuilder builder = new ExportReportBuilder(snapshot);
        List<EntryBytes> entries = new ArrayList<>();
        addCsv(entries, "updated_assets.csv", builder.updatedAssets(), options);
        addCsv(entries, "audit_summary.csv", builder.auditSummary(), options);
        addCsv(entries, "department_summary.csv", builder.departmentSummary(), options);
        addCsv(entries, "exception_summary.csv", builder.exceptionSummary(), options);
        addCsv(entries, "moved_assets.csv", builder.assetsByStatus("MOVED"), options);
        addCsv(entries, "new_assets.csv", builder.assetsByStatus("NEW"), options);
        addCsv(entries, "missing_assets.csv", builder.assetsByStatus("MISSING"), options);
        addCsv(entries, "skipped_assets.csv", builder.assetsByStatus("SKIPPED"), options);
        addCsv(entries, "duplicate_scans.csv", builder.eventsByResult("DUPLICATE_SCAN"), options);
        addCsv(entries, "invalid_scans.csv", builder.eventsByResult("INVALID_SCAN"), options);
        addCsv(entries, "unresolved_assets.csv", builder.assetsByStatus("UNRESOLVED"), options);
        addCsv(entries, "audit_event_log.csv", builder.auditEvents(), options);
        addCsv(entries, "import_issues.csv", builder.importIssues(), options);
        if (options.isIncludeReadme()) {
            byte[] readme = builder.readme().getBytes(java.nio.charset.StandardCharsets.UTF_8);
            entries.add(new EntryBytes("README.txt", "text/plain", 1, readme));
        }
        return entries;
    }

    private void addCsv(List<EntryBytes> entries, String name, List<String[]> rows, ExportOptions options) {
        entries.add(new EntryBytes(name, "text/csv", Math.max(0, rows.size() - 1),
                csvWriter.write(rows, options.isExcelFriendlyCsv())));
    }

    private String sha256(byte[] bytes) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] hash = digest.digest(bytes);
        StringBuilder builder = new StringBuilder();
        for (byte b : hash) {
            builder.append(String.format("%02x", b));
        }
        return builder.toString();
    }

    private static final class EntryBytes {
        final String name;
        final String mediaType;
        final int rowCount;
        final byte[] bytes;

        EntryBytes(String name, String mediaType, int rowCount, byte[] bytes) {
            this.name = name;
            this.mediaType = mediaType;
            this.rowCount = rowCount;
            this.bytes = bytes;
        }
    }
}
