package uk.co.hsim.assetaudit.export;

import java.util.List;

import uk.co.hsim.assetaudit.data.entity.AssetEntity;
import uk.co.hsim.assetaudit.data.entity.AuditEventEntity;
import uk.co.hsim.assetaudit.data.entity.AuditSessionEntity;
import uk.co.hsim.assetaudit.data.entity.DepartmentAuditEntity;
import uk.co.hsim.assetaudit.data.entity.ImportIssueEntity;

public final class ExportSnapshot {
    public final String packageId;
    public final long exportedAtUtc;
    public final ExportPreview preview;
    public final AuditSessionEntity session;
    public final List<AssetEntity> assets;
    public final List<DepartmentAuditEntity> departments;
    public final List<AuditEventEntity> events;
    public final List<ImportIssueEntity> importIssues;

    public ExportSnapshot(String packageId, long exportedAtUtc, ExportPreview preview,
                          AuditSessionEntity session, List<AssetEntity> assets,
                          List<DepartmentAuditEntity> departments, List<AuditEventEntity> events,
                          List<ImportIssueEntity> importIssues) {
        this.packageId = packageId;
        this.exportedAtUtc = exportedAtUtc;
        this.preview = preview;
        this.session = session;
        this.assets = assets;
        this.departments = departments;
        this.events = events;
        this.importIssues = importIssues;
    }
}
