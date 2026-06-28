package uk.co.hsim.assetaudit.export;

import java.util.UUID;

import uk.co.hsim.assetaudit.data.db.AuditDatabase;
import uk.co.hsim.assetaudit.data.entity.AuditSessionEntity;
import uk.co.hsim.assetaudit.domain.results.ErrorCode;
import uk.co.hsim.assetaudit.domain.results.OperationResult;
import uk.co.hsim.assetaudit.util.clock.Clock;

public final class ExportSnapshotBuilder {
    private final AuditDatabase database;
    private final ReportPreviewService previewService;
    private final Clock clock;

    public ExportSnapshotBuilder(AuditDatabase database, ReportPreviewService previewService, Clock clock) {
        this.database = database;
        this.previewService = previewService;
        this.clock = clock;
    }

    public OperationResult<ExportSnapshot> build(String sessionId, boolean allowDraft) {
        OperationResult<ExportPreview> preview = previewService.buildPreview(sessionId, allowDraft);
        if (!preview.isSuccess()) {
            return OperationResult.fail(preview.getErrorCode(), preview.getMessage());
        }
        if (preview.getValue().getReadiness() == ExportReadinessLevel.BLOCKED) {
            return OperationResult.fail(ErrorCode.INVALID_STATE, "Export is blocked by unresolved audit state.");
        }
        AuditSessionEntity session = database.auditSessionDao().getById(sessionId);
        return OperationResult.ok(new ExportSnapshot(
                UUID.randomUUID().toString(),
                clock.nowUtcMillis(),
                preview.getValue(),
                session,
                database.assetDao().listBySessionForExport(sessionId),
                database.departmentAuditDao().listBySession(sessionId),
                database.auditEventDao().listBySession(sessionId),
                database.importIssueDao().listBySession(sessionId)
        ));
    }
}
