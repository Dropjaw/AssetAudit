package uk.co.hsim.assetaudit.hardening;

import android.net.Uri;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.regex.Pattern;

import uk.co.hsim.assetaudit.scanner.ScannerPayload;

public final class DiagnosticRedactor {
    private static final Pattern CONTENT_URI = Pattern.compile("content://\\S+", Pattern.CASE_INSENSITIVE);
    private static final Pattern FILE_URI = Pattern.compile("file://\\S+", Pattern.CASE_INSENSITIVE);
    private static final Pattern WINDOWS_PATH = Pattern.compile("[A-Za-z]:\\\\[^\\s]+");
    private static final Pattern UNC_PATH = Pattern.compile("\\\\\\\\[^\\s]+");
    private static final Pattern CONTROL_CHARS = Pattern.compile("[\\p{Cntrl}&&[^\r\n\t]]");

    public String redactMessage(String message) {
        if (message == null) {
            return null;
        }
        String redacted = CONTENT_URI.matcher(message).replaceAll("content://[redacted]");
        redacted = FILE_URI.matcher(redacted).replaceAll("file://[redacted]");
        redacted = WINDOWS_PATH.matcher(redacted).replaceAll("[path-redacted]");
        redacted = UNC_PATH.matcher(redacted).replaceAll("[path-redacted]");
        return CONTROL_CHARS.matcher(redacted).replaceAll("?");
    }

    public String redactAssetTag(String value) {
        if (value == null || value.trim().isEmpty()) {
            return "";
        }
        String trimmed = value.trim();
        String suffix = trimmed.length() <= 4 ? trimmed : trimmed.substring(trimmed.length() - 4);
        return "tag ****" + suffix + " #" + hashPrefix(trimmed);
    }

    public String redactUri(String uriOrPath) {
        if (uriOrPath == null || uriOrPath.trim().isEmpty()) {
            return "";
        }
        return "destination #" + hashPrefix(uriOrPath.trim());
    }

    public String safeDisplayName(String displayName) {
        if (displayName == null || displayName.trim().isEmpty()) {
            return "unnamed";
        }
        String name = displayName.trim();
        int slash = Math.max(name.lastIndexOf('/'), name.lastIndexOf('\\'));
        if (slash >= 0 && slash + 1 < name.length()) {
            name = name.substring(slash + 1);
        }
        name = CONTROL_CHARS.matcher(name).replaceAll("?");
        if (name.length() > 80) {
            name = name.substring(0, 80);
        }
        return name;
    }

    public String scannerPayloadSummary(ScannerPayload payload) {
        if (payload == null) {
            return "payload=missing";
        }
        return "payload=" + redactAssetTag(payload.getData())
                + " label=" + safeDisplayName(payload.getSymbology())
                + " source=" + safeDisplayName(payload.getSource());
    }

    public String exportDestinationSummary(Uri uri, String fallbackDisplayName) {
        String safeName = safeDisplayName(fallbackDisplayName);
        if (uri == null) {
            return safeName;
        }
        return safeName + " (" + redactUri(uri.toString()) + ")";
    }

    private String hashPrefix(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(value.getBytes(StandardCharsets.UTF_8));
            StringBuilder builder = new StringBuilder();
            for (int i = 0; i < 4 && i < hash.length; i++) {
                builder.append(String.format("%02x", hash[i]));
            }
            return builder.toString();
        } catch (Exception e) {
            return "unknown";
        }
    }
}
