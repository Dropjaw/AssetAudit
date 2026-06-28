package uk.co.hsim.assetaudit.export;

import java.util.ArrayList;
import java.util.List;

import uk.co.hsim.assetaudit.data.db.AuditDatabase;
import uk.co.hsim.assetaudit.data.entity.AuditSessionEntity;
import uk.co.hsim.assetaudit.data.entity.DepartmentAuditEntity;
import uk.co.hsim.assetaudit.domain.enums.AuditStatus;
import uk.co.hsim.assetaudit.domain.enums.DepartmentAuditStatus;
import uk.co.hsim.assetaudit.domain.enums.ImportIssueSeverity;
import uk.co.hsim.assetaudit.domain.enums.ScanResultType;
import uk.co.hsim.assetaudit.domain.enums.SessionStatus;
import uk.co.hsim.assetaudit.domain.results.ErrorCode;
import uk.co.hsim.assetaudit.domain.results.OperationResult;
import uk.co.hsim.assetaudit.util.clock.Clock;

public final class ReportPreviewService {
    private final AuditDatabase database;
    private final Clock clock;

    public ReportPreviewService(AuditDatabase database, Clock clock) {
        this.database = database;
        this.clock = clock;
    }

    public OperationResult<ExportPreview> buildPreview(String sessionId, boolean allowDraft) {
        AuditSessionEntity session = database.auditSessionDao().getById(sessionId);
        if (session == null) {
            return OperationResult.fail(ErrorCode.NOT_FOUND, "No active audit session is available.");
        }
        List<DepartmentAuditEntity> departments = database.departmentAuditDao().listBySession(sessionId);
        int total = database.assetDao().countBySession(sessionId);
        int remaining = database.assetDao().countByStatus(sessionId, AuditStatus.NOT_AUDITED);
        int exceptions = database.assetDao().countByStatus(sessionId, AuditStatus.AUDITED_MOVED_DEPARTMENT)
                + database.assetDao().countByStatus(sessionId, AuditStatus.NEW_ASSET_ADDED)
                + database.assetDao().countByStatus(sessionId, AuditStatus.MISSING_AFTER_DEPARTMENT_CLOSE)
                + database.assetDao().countByStatus(sessionId, AuditStatus.SKIPPED_UNABLE_TO_VERIFY);
        int duplicates = database.auditEventDao().countByResult(sessionId, ScanResultType.DUPLICATE_SCAN);
        int invalid = database.auditEventDao().countByResult(sessionId, ScanResultType.INVALID_SCAN);
        int incompleteDepartments = database.departmentAuditDao().countIncompleteDepartments(sessionId);
        boolean allDepartmentsComplete = database.departmentAuditDao().countDepartments(sessionId) > 0
                && incompleteDepartments == 0;
        boolean finalExportRecorded = database.exportRunDao().countFinalExports(sessionId) > 0;
        boolean forceClosed = session.status == SessionStatus.FORCE_CLOSED;
        List<String> warnings = new ArrayList<>();
        if (remaining > 0) {
            warnings.add(remaining + " assets remain not audited.");
        }
        for (DepartmentAuditEntity department : departments) {
            if (department.status != DepartmentAuditStatus.COMPLETE
                    && department.status != DepartmentAuditStatus.COMPLETE_WITH_EXCEPTIONS) {
                warnings.add("Department not complete: " + department.departmentName);
            }
        }
        int importWarnings = database.importIssueDao().countBySeverity(sessionId, ImportIssueSeverity.WARNING)
                + database.importIssueDao().countBySeverity(sessionId, ImportIssueSeverity.INFO);
        if (importWarnings > 0) {
            warnings.add(importWarnings + " import warnings/info issues are included.");
        }
        if (forceClosed) {
            warnings.add("Audit was force closed by user.");
        }
        ExportReadinessLevel readiness;
        if (total == 0) {
            readiness = ExportReadinessLevel.BLOCKED;
            warnings.add("No assets are available to export.");
        } else if (remaining > 0) {
            readiness = allowDraft ? ExportReadinessLevel.DRAFT_INCOMPLETE : ExportReadinessLevel.BLOCKED;
        } else if (exceptions > 0 || duplicates > 0 || invalid > 0 || importWarnings > 0) {
            readiness = ExportReadinessLevel.READY_WITH_WARNINGS;
        } else {
            readiness = ExportReadinessLevel.READY;
        }
        return OperationResult.ok(new ExportPreview(session.sessionId, session.auditName, session.sourceFileName,
                clock.nowUtcMillis(), readiness, total, remaining, exceptions, duplicates, invalid,
                session.status, session.completedAtUtc, allDepartmentsComplete, finalExportRecorded,
                forceClosed, departments, warnings));
    }
}
