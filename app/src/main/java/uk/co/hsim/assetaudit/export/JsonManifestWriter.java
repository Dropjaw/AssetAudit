package uk.co.hsim.assetaudit.export;

import java.util.List;

import uk.co.hsim.assetaudit.util.device.DeviceInfoProvider;

public final class JsonManifestWriter {
    public String write(ExportSnapshot snapshot, ExportOptions options, List<ExportFileRecord> files,
                        DeviceInfoProvider deviceInfoProvider) {
        StringBuilder builder = new StringBuilder();
        builder.append("{\n");
        append(builder, "schemaVersion", "1", false, true);
        append(builder, "packageId", snapshot.packageId, true, true);
        append(builder, "appPackage", deviceInfoProvider.getPackageName(), true, true);
        append(builder, "appVersionName", deviceInfoProvider.getAppVersionName(), true, true);
        append(builder, "sessionId", snapshot.session.sessionId, true, true);
        append(builder, "auditName", snapshot.session.auditName, true, true);
        append(builder, "sourceFileName", snapshot.session.sourceFileName, true, true);
        append(builder, "exportedAtUtc", String.valueOf(snapshot.exportedAtUtc), false, true);
        append(builder, "exportMode", options.getExportMode().name(), true, true);
        append(builder, "readiness", snapshot.preview.getReadiness().name(), true, true);
        builder.append("  \"files\": [\n");
        for (int i = 0; i < files.size(); i++) {
            ExportFileRecord file = files.get(i);
            builder.append("    {\"path\":\"").append(escape(file.getFileName())).append("\",\"rowCount\":")
                    .append(file.getRowCount()).append(",\"sha256\":\"").append(file.getSha256()).append("\"}");
            builder.append(i + 1 == files.size() ? "\n" : ",\n");
        }
        builder.append("  ],\n");
        builder.append("  \"warnings\": [");
        for (int i = 0; i < snapshot.preview.getWarnings().size(); i++) {
            if (i > 0) builder.append(',');
            builder.append('"').append(escape(snapshot.preview.getWarnings().get(i))).append('"');
        }
        builder.append("]\n}");
        return builder.toString();
    }

    private void append(StringBuilder builder, String key, String value, boolean quote, boolean comma) {
        builder.append("  \"").append(key).append("\": ");
        if (quote) builder.append('"').append(escape(value)).append('"');
        else builder.append(value == null || value.isEmpty() ? "0" : value);
        if (comma) builder.append(',');
        builder.append('\n');
    }

    private String escape(String value) {
        return value == null ? "" : value.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
