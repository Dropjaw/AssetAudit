package uk.co.hsim.assetaudit.export;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import uk.co.hsim.assetaudit.data.entity.DepartmentAuditEntity;

public final class ExportPreview {
    private final String sessionId;
    private final String auditName;
    private final String sourceFileName;
    private final long generatedAtUtc;
    private final ExportReadinessLevel readiness;
    private final int totalAssets;
    private final int remainingAssets;
    private final int exceptionAssets;
    private final int duplicateScans;
    private final int invalidScans;
    private final List<DepartmentAuditEntity> departments;
    private final List<String> warnings;

    public ExportPreview(String sessionId, String auditName, String sourceFileName, long generatedAtUtc,
                         ExportReadinessLevel readiness, int totalAssets, int remainingAssets,
                         int exceptionAssets, int duplicateScans, int invalidScans,
                         List<DepartmentAuditEntity> departments, List<String> warnings) {
        this.sessionId = sessionId;
        this.auditName = auditName;
        this.sourceFileName = sourceFileName;
        this.generatedAtUtc = generatedAtUtc;
        this.readiness = readiness;
        this.totalAssets = totalAssets;
        this.remainingAssets = remainingAssets;
        this.exceptionAssets = exceptionAssets;
        this.duplicateScans = duplicateScans;
        this.invalidScans = invalidScans;
        this.departments = Collections.unmodifiableList(new ArrayList<>(departments));
        this.warnings = Collections.unmodifiableList(new ArrayList<>(warnings));
    }

    public String getSessionId() { return sessionId; }
    public String getAuditName() { return auditName; }
    public String getSourceFileName() { return sourceFileName; }
    public long getGeneratedAtUtc() { return generatedAtUtc; }
    public ExportReadinessLevel getReadiness() { return readiness; }
    public int getTotalAssets() { return totalAssets; }
    public int getRemainingAssets() { return remainingAssets; }
    public int getExceptionAssets() { return exceptionAssets; }
    public int getDuplicateScans() { return duplicateScans; }
    public int getInvalidScans() { return invalidScans; }
    public List<DepartmentAuditEntity> getDepartments() { return departments; }
    public List<String> getWarnings() { return warnings; }
}
