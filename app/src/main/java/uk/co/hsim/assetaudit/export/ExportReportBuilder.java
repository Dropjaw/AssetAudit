package uk.co.hsim.assetaudit.export;

import java.util.ArrayList;
import java.util.List;

import uk.co.hsim.assetaudit.data.entity.AssetEntity;
import uk.co.hsim.assetaudit.data.entity.AuditEventEntity;
import uk.co.hsim.assetaudit.data.entity.DepartmentAuditEntity;
import uk.co.hsim.assetaudit.data.entity.ImportIssueEntity;
import uk.co.hsim.assetaudit.domain.enums.AuditStatus;
import uk.co.hsim.assetaudit.domain.enums.EventKind;
import uk.co.hsim.assetaudit.domain.enums.ScanResultType;

public final class ExportReportBuilder {
    private final ExportSnapshot snapshot;

    public ExportReportBuilder(ExportSnapshot snapshot) {
        this.snapshot = snapshot;
    }

    public List<String[]> updatedAssets() {
        List<String[]> rows = new ArrayList<>();
        rows.add(new String[]{"asset_tag_id", "source_row_number", "source_file_name", "description", "brand", "model",
                "status", "category", "sub_category", "site", "location", "department", "previous_department",
                "owner", "primary_user", "audit_status", "created_during_audit", "updated_at_utc",
                "source_audit_details", "exported_at_utc", "export_package_id"});
        for (AssetEntity a : snapshot.assets) {
            rows.add(new String[]{a.assetTagId, text(a.sourceRowNumber), snapshot.session.sourceFileName,
                    a.description, a.brand, a.model, a.status, a.category, a.subCategory, a.site,
                    a.location, a.department, a.previousDepartment, a.owner, a.primaryUser,
                    a.auditStatus.name(), String.valueOf(a.createdDuringAudit), String.valueOf(a.updatedAtUtc),
                    a.auditDetails, String.valueOf(snapshot.exportedAtUtc), snapshot.packageId});
        }
        return rows;
    }

    public List<String[]> auditSummary() {
        List<String[]> rows = new ArrayList<>();
        rows.add(new String[]{"session_id", "audit_name", "source_file_name", "readiness", "total_assets",
                "remaining_assets", "exception_assets", "duplicate_scans", "invalid_scans", "exported_at_utc"});
        rows.add(new String[]{snapshot.session.sessionId, snapshot.session.auditName, snapshot.session.sourceFileName,
                snapshot.preview.getReadiness().name(), String.valueOf(snapshot.preview.getTotalAssets()),
                String.valueOf(snapshot.preview.getRemainingAssets()), String.valueOf(snapshot.preview.getExceptionAssets()),
                String.valueOf(snapshot.preview.getDuplicateScans()), String.valueOf(snapshot.preview.getInvalidScans()),
                String.valueOf(snapshot.exportedAtUtc)});
        return rows;
    }

    public List<String[]> departmentSummary() {
        List<String[]> rows = new ArrayList<>();
        rows.add(new String[]{"department", "expected_count", "scanned_count", "remaining_count", "moved_in_count",
                "new_asset_count", "missing_count", "skipped_count", "duplicate_scan_count", "invalid_scan_count",
                "status", "completed_at_utc"});
        for (DepartmentAuditEntity d : snapshot.departments) {
            rows.add(new String[]{d.departmentName, String.valueOf(d.expectedCount), String.valueOf(d.scannedCount),
                    String.valueOf(countStatus(d.departmentName, AuditStatus.NOT_AUDITED)), String.valueOf(d.movedInCount),
                    String.valueOf(d.newAssetCount), String.valueOf(d.missingCount),
                    String.valueOf(countStatus(d.departmentName, AuditStatus.SKIPPED_UNABLE_TO_VERIFY)),
                    String.valueOf(countResult(d.departmentName, ScanResultType.DUPLICATE_SCAN)),
                    String.valueOf(countResult(d.departmentName, ScanResultType.INVALID_SCAN)),
                    d.status.name(), text(d.completedAtUtc)});
        }
        return rows;
    }

    public List<String[]> exceptionSummary() {
        List<String[]> rows = new ArrayList<>();
        rows.add(new String[]{"exception_type", "department", "count", "severity", "requires_follow_up", "notes"});
        addSummaryRows(rows, "MOVED_DEPARTMENT", AuditStatus.AUDITED_MOVED_DEPARTMENT, "REVIEW", true);
        addSummaryRows(rows, "NEW_ASSET", AuditStatus.NEW_ASSET_ADDED, "REVIEW", true);
        addSummaryRows(rows, "MISSING", AuditStatus.MISSING_AFTER_DEPARTMENT_CLOSE, "REVIEW", true);
        addSummaryRows(rows, "SKIPPED", AuditStatus.SKIPPED_UNABLE_TO_VERIFY, "REVIEW", true);
        addEventRows(rows, "DUPLICATE_SCAN", ScanResultType.DUPLICATE_SCAN, "INFO", false);
        addEventRows(rows, "INVALID_SCAN", ScanResultType.INVALID_SCAN, "INFO", false);
        addSummaryRows(rows, "UNRESOLVED", AuditStatus.NOT_AUDITED, "REVIEW", true);
        return rows;
    }

    public List<String[]> assetsByStatus(String type) {
        List<String[]> rows = new ArrayList<>();
        rows.add(new String[]{"asset_tag_id", "department", "previous_department", "description", "location", "owner",
                "primary_user", "audit_status", "notes"});
        for (AssetEntity a : snapshot.assets) {
            if (matchesType(a, type)) {
                rows.add(new String[]{a.assetTagId, a.department, a.previousDepartment, a.description, a.location,
                        a.owner, a.primaryUser, a.auditStatus.name(), latestNotes(a.assetTagId)});
            }
        }
        return rows;
    }

    public List<String[]> eventsByResult(String resultName) {
        List<String[]> rows = new ArrayList<>();
        rows.add(new String[]{"timestamp_utc", "asset_tag_id", "selected_department", "previous_department",
                "event_kind", "result_type", "user_name", "device_id", "notes"});
        for (AuditEventEntity e : snapshot.events) {
            if (e.resultType != null && e.resultType.name().equals(resultName)) {
                rows.add(eventRow(e));
            }
        }
        return rows;
    }

    public List<String[]> auditEvents() {
        List<String[]> rows = new ArrayList<>();
        rows.add(new String[]{"event_id", "session_id", "timestamp_utc", "event_kind", "result_type", "asset_tag_id",
                "selected_department", "previous_department", "user_name", "device_id", "notes"});
        for (AuditEventEntity e : snapshot.events) {
            rows.add(new String[]{e.eventId, e.sessionId, String.valueOf(e.timestampUtc), e.eventKind.name(),
                    e.resultType == null ? "" : e.resultType.name(), e.assetTagId, e.selectedDepartment,
                    e.previousDepartment, e.userName, e.deviceId, e.notes});
        }
        return rows;
    }

    public List<String[]> importIssues() {
        List<String[]> rows = new ArrayList<>();
        rows.add(new String[]{"row_number", "column_name", "severity", "issue_code", "source_value", "message", "resolved"});
        for (ImportIssueEntity issue : snapshot.importIssues) {
            rows.add(new String[]{text(issue.rowNumber), issue.columnName, issue.severity.name(), issue.issueCode,
                    issue.sourceValue, issue.message, String.valueOf(issue.resolved)});
        }
        return rows;
    }

    public String readme() {
        return "Asset Audit export package\r\nAudit: " + snapshot.session.auditName
                + "\r\nPackage: " + snapshot.packageId
                + "\r\nThis package was generated from local Room audit state. The original import file was not modified.\r\n";
    }

    private void addSummaryRows(List<String[]> rows, String type, AuditStatus status, String severity, boolean followUp) {
        for (DepartmentAuditEntity d : snapshot.departments) {
            int count = countStatus(d.departmentName, status);
            if (count > 0 || rows.size() == 1) {
                rows.add(new String[]{type, d.departmentName, String.valueOf(count), severity,
                        String.valueOf(followUp), count + " " + type + " assets in " + d.departmentName});
            }
        }
    }

    private void addEventRows(List<String[]> rows, String type, ScanResultType result, String severity, boolean followUp) {
        for (DepartmentAuditEntity d : snapshot.departments) {
            int count = countResult(d.departmentName, result);
            if (count > 0) {
                rows.add(new String[]{type, d.departmentName, String.valueOf(count), severity,
                        String.valueOf(followUp), count + " " + type + " events in " + d.departmentName});
            }
        }
    }

    private boolean matchesType(AssetEntity a, String type) {
        if ("MOVED".equals(type)) return a.auditStatus == AuditStatus.AUDITED_MOVED_DEPARTMENT;
        if ("NEW".equals(type)) return a.auditStatus == AuditStatus.NEW_ASSET_ADDED || a.createdDuringAudit;
        if ("MISSING".equals(type)) return a.auditStatus == AuditStatus.MISSING_AFTER_DEPARTMENT_CLOSE;
        if ("SKIPPED".equals(type)) return a.auditStatus == AuditStatus.SKIPPED_UNABLE_TO_VERIFY;
        if ("UNRESOLVED".equals(type)) return a.auditStatus == AuditStatus.NOT_AUDITED;
        return false;
    }

    private int countStatus(String department, AuditStatus status) {
        int count = 0;
        for (AssetEntity a : snapshot.assets) {
            if (status == a.auditStatus && safeEquals(department, a.department)) count++;
        }
        return count;
    }

    private int countResult(String department, ScanResultType result) {
        int count = 0;
        for (AuditEventEntity e : snapshot.events) {
            if (result == e.resultType && safeEquals(department, e.selectedDepartment)) count++;
        }
        return count;
    }

    private String latestNotes(String assetTagId) {
        for (int i = snapshot.events.size() - 1; i >= 0; i--) {
            AuditEventEntity e = snapshot.events.get(i);
            if (safeEquals(assetTagId, e.assetTagId) && e.eventKind != EventKind.SCAN_REJECTED) {
                return e.notes;
            }
        }
        return "";
    }

    private String[] eventRow(AuditEventEntity e) {
        return new String[]{String.valueOf(e.timestampUtc), e.assetTagId, e.selectedDepartment, e.previousDepartment,
                e.eventKind.name(), e.resultType == null ? "" : e.resultType.name(), e.userName, e.deviceId, e.notes};
    }

    private String text(Object value) {
        return value == null ? "" : String.valueOf(value);
    }

    private boolean safeEquals(String left, String right) {
        return left == null ? right == null : left.equals(right);
    }
}
