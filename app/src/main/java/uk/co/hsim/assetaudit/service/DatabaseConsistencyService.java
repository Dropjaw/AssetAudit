package uk.co.hsim.assetaudit.service;

import java.util.ArrayList;
import java.util.List;

import uk.co.hsim.assetaudit.data.db.AuditDatabase;
import uk.co.hsim.assetaudit.data.entity.AssetEntity;
import uk.co.hsim.assetaudit.data.entity.DepartmentAuditEntity;
import uk.co.hsim.assetaudit.data.entity.ExportRunEntity;
import uk.co.hsim.assetaudit.domain.enums.AuditStatus;
import uk.co.hsim.assetaudit.domain.enums.EventKind;
import uk.co.hsim.assetaudit.hardening.DiagnosticRedactor;

public final class DatabaseConsistencyService {
    private final AuditDatabase database;
    private final DiagnosticRedactor redactor;

    public DatabaseConsistencyService(AuditDatabase database) {
        this.database = database;
        this.redactor = new DiagnosticRedactor();
    }

    public List<String> checkSession(String sessionId) {
        List<String> warnings = new ArrayList<>();
        if (sessionId == null || sessionId.trim().isEmpty()) {
            warnings.add("Missing session id.");
            return warnings;
        }
        for (DepartmentAuditEntity department : database.departmentAuditDao().listBySession(sessionId)) {
            int remaining = database.assetDao().countByDepartmentAndStatus(
                    sessionId, department.departmentName, AuditStatus.NOT_AUDITED);
            int missing = database.assetDao().countByDepartmentAndStatus(
                    sessionId, department.departmentName, AuditStatus.MISSING_AFTER_DEPARTMENT_CLOSE);
            if (missing != department.missingCount) {
                warnings.add("Missing count drift for department " + department.departmentName);
            }
            if (remaining == 0 && department.scannedCount > department.expectedCount + department.movedInCount) {
                warnings.add("Scanned count drift for department " + department.departmentName);
            }
        }
        for (AssetEntity asset : database.assetDao().listBySessionForExport(sessionId)) {
            if (asset.auditStatus == AuditStatus.AUDITED_EXPECTED
                    && database.auditEventDao().countByAssetAndKind(sessionId, asset.assetTagId, EventKind.SCAN_ACCEPTED) == 0) {
                warnings.add("Expected scan evidence missing for " + redactor.redactAssetTag(asset.assetTagId));
            }
            if (asset.auditStatus == AuditStatus.AUDITED_MOVED_DEPARTMENT
                    && (asset.previousDepartment == null || asset.previousDepartment.trim().isEmpty()
                    || database.auditEventDao().countByAssetAndKind(sessionId, asset.assetTagId, EventKind.MOVEMENT_CONFIRMED) == 0)) {
                warnings.add("Movement evidence missing for " + redactor.redactAssetTag(asset.assetTagId));
            }
            if (asset.createdDuringAudit
                    && database.auditEventDao().countByAssetAndKind(sessionId, asset.assetTagId, EventKind.NEW_ASSET_CREATED) == 0) {
                warnings.add("New asset evidence missing for " + redactor.redactAssetTag(asset.assetTagId));
            }
        }
        for (ExportRunEntity run : database.exportRunDao().listRecentRuns(sessionId, 100)) {
            if (database.exportRunDao().countFiles(run.exportRunId) == 0) {
                warnings.add("Export run has no file records: " + run.packageId);
            }
        }
        return warnings;
    }
}
