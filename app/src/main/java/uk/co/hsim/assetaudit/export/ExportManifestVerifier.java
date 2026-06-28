package uk.co.hsim.assetaudit.export;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import uk.co.hsim.assetaudit.domain.results.ErrorCode;
import uk.co.hsim.assetaudit.domain.results.OperationResult;

public final class ExportManifestVerifier {
    public OperationResult<String> verify(List<ExportFileRecord> files) {
        if (files == null || files.isEmpty()) {
            return OperationResult.fail(ErrorCode.VALIDATION_FAILED, "Export manifest has no files.");
        }
        Set<String> names = new HashSet<>();
        for (ExportFileRecord file : files) {
            if (file == null) {
                return OperationResult.fail(ErrorCode.VALIDATION_FAILED, "Export manifest contains a blank file record.");
            }
            if (!isSafeZipEntryName(file.getFileName())) {
                return OperationResult.fail(ErrorCode.VALIDATION_FAILED, "Export file name is not safe.");
            }
            if (file.getRowCount() < 0) {
                return OperationResult.fail(ErrorCode.VALIDATION_FAILED, "Export row count is invalid.");
            }
            if (!isSha256(file.getSha256())) {
                return OperationResult.fail(ErrorCode.VALIDATION_FAILED, "Export file hash is invalid.");
            }
            if (!names.add(file.getFileName())) {
                return OperationResult.fail(ErrorCode.VALIDATION_FAILED, "Export manifest contains duplicate file names.");
            }
        }
        return OperationResult.ok("Export manifest verified.");
    }

    private boolean isSafeZipEntryName(String name) {
        if (name == null || name.isEmpty() || name.length() > 120) {
            return false;
        }
        if (name.startsWith("/") || name.startsWith("\\") || name.contains("..")) {
            return false;
        }
        for (int i = 0; i < name.length(); i++) {
            char c = name.charAt(i);
            boolean allowed = (c >= 'A' && c <= 'Z')
                    || (c >= 'a' && c <= 'z')
                    || (c >= '0' && c <= '9')
                    || c == '_' || c == '-' || c == '.';
            if (!allowed) {
                return false;
            }
        }
        return true;
    }

    private boolean isSha256(String value) {
        if (value == null || value.length() != 64) {
            return false;
        }
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            if (!((c >= '0' && c <= '9') || (c >= 'a' && c <= 'f'))) {
                return false;
            }
        }
        return true;
    }
}
