package uk.co.hsim.assetaudit.service;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import uk.co.hsim.assetaudit.data.db.AuditDatabase;
import uk.co.hsim.assetaudit.data.entity.AssetEntity;
import uk.co.hsim.assetaudit.data.entity.AuditEventEntity;
import uk.co.hsim.assetaudit.data.entity.DepartmentAuditEntity;
import uk.co.hsim.assetaudit.domain.enums.AuditStatus;
import uk.co.hsim.assetaudit.domain.enums.DepartmentAuditStatus;
import uk.co.hsim.assetaudit.domain.enums.EventKind;
import uk.co.hsim.assetaudit.domain.enums.ScanResultType;
import uk.co.hsim.assetaudit.domain.results.ErrorCode;
import uk.co.hsim.assetaudit.domain.results.OperationResult;
import uk.co.hsim.assetaudit.util.clock.Clock;
import uk.co.hsim.assetaudit.util.device.DeviceInfoProvider;
import uk.co.hsim.assetaudit.util.identity.UserIdentityProvider;

public final class ExceptionResolutionService {
    private final AuditDatabase database;
    private final DepartmentSummaryService departmentSummaryService;
    private final DiagnosticService diagnosticService;
    private final Clock clock;
    private final UserIdentityProvider userIdentityProvider;
    private final DeviceInfoProvider deviceInfoProvider;

    public ExceptionResolutionService(AuditDatabase database, DepartmentSummaryService departmentSummaryService,
                                      DiagnosticService diagnosticService, Clock clock,
                                      UserIdentityProvider userIdentityProvider,
                                      DeviceInfoProvider deviceInfoProvider) {
        this.database = database;
        this.departmentSummaryService = departmentSummaryService;
        this.diagnosticService = diagnosticService;
        this.clock = clock;
        this.userIdentityProvider = userIdentityProvider;
        this.deviceInfoProvider = deviceInfoProvider;
    }

    public OperationResult<ExceptionResolutionResult> confirmMovement(MovementConfirmationRequest request) {
        try {
            AtomicReference<OperationResult<ExceptionResolutionResult>> result = new AtomicReference<>();
            database.runInTransaction(() -> result.set(confirmMovementInTransaction(request)));
            return result.get();
        } catch (RuntimeException e) {
            diagnosticService.logError("Exceptions", "MOVEMENT_CONFIRM_FAILED tag=" + safeTag(request.getAssetTagId()), e);
            return OperationResult.fail(ErrorCode.DATABASE_ERROR, "Movement could not be confirmed.");
        }
    }

    public OperationResult<ExceptionResolutionResult> createNewAsset(NewAssetDraft draft) {
        String validation = validateNewAssetDraft(draft);
        if (validation != null) {
            return OperationResult.fail(ErrorCode.VALIDATION_FAILED, validation);
        }
        try {
            AtomicReference<OperationResult<ExceptionResolutionResult>> result = new AtomicReference<>();
            database.runInTransaction(() -> result.set(createNewAssetInTransaction(draft)));
            return result.get();
        } catch (RuntimeException e) {
            diagnosticService.logError("Exceptions", "NEW_ASSET_CREATE_FAILED tag=" + safeTag(draft.getAssetTagId()), e);
            return OperationResult.fail(ErrorCode.DATABASE_ERROR, "New asset could not be created.");
        }
    }

    public OperationResult<FinishDepartmentPreview> previewFinishDepartment(String sessionId, String department) {
        DepartmentAuditEntity departmentAudit = database.departmentAuditDao().getDepartment(sessionId, department);
        if (departmentAudit == null) {
            return OperationResult.fail(ErrorCode.NOT_FOUND, "Department is not available.");
        }
        List<AssetEntity> remaining = database.assetDao().listByDepartmentAndStatus(
                sessionId,
                department,
                AuditStatus.NOT_AUDITED
        );
        return OperationResult.ok(new FinishDepartmentPreview(
                sessionId,
                department,
                departmentAudit.expectedCount,
                departmentAudit.scannedCount,
                remaining,
                buildExceptionCounts(sessionId, department, departmentAudit)
        ));
    }

    public OperationResult<ExceptionResolutionResult> markRemainingMissing(String sessionId, String department,
                                                                          List<String> assetTagIds) {
        if (assetTagIds == null || assetTagIds.isEmpty()) {
            return OperationResult.fail(ErrorCode.VALIDATION_FAILED, "Select at least one remaining asset.");
        }
        try {
            AtomicReference<OperationResult<ExceptionResolutionResult>> result = new AtomicReference<>();
            database.runInTransaction(() -> result.set(markMissingInTransaction(sessionId, department, assetTagIds)));
            return result.get();
        } catch (RuntimeException e) {
            diagnosticService.logError("Exceptions", "MISSING_MARK_FAILED department=" + department, e);
            return OperationResult.fail(ErrorCode.DATABASE_ERROR, "Missing assets could not be saved.");
        }
    }

    public OperationResult<ExceptionResolutionResult> skipAssets(SkipAssetsRequest request) {
        if (request.getReason().isEmpty()) {
            return OperationResult.fail(ErrorCode.VALIDATION_FAILED, "Skip reason is required.");
        }
        if (request.getAssetTagIds().isEmpty()) {
            return OperationResult.fail(ErrorCode.VALIDATION_FAILED, "Select at least one remaining asset.");
        }
        try {
            AtomicReference<OperationResult<ExceptionResolutionResult>> result = new AtomicReference<>();
            database.runInTransaction(() -> result.set(skipAssetsInTransaction(request)));
            return result.get();
        } catch (RuntimeException e) {
            diagnosticService.logError("Exceptions", "SKIP_MARK_FAILED department=" + request.getDepartment(), e);
            return OperationResult.fail(ErrorCode.DATABASE_ERROR, "Skipped assets could not be saved.");
        }
    }

    public OperationResult<DuplicateReviewState> buildDuplicateReview(String sessionId, String assetTagId) {
        AssetEntity asset = database.assetDao().getByTag(sessionId, assetTagId);
        if (asset == null) {
            return OperationResult.fail(ErrorCode.NOT_FOUND, "Duplicate asset is no longer available.");
        }
        return OperationResult.ok(new DuplicateReviewState(
                asset,
                database.auditEventDao().listRecentByAssetTag(sessionId, assetTagId, 5)
        ));
    }

    public String validateNewAssetDraft(NewAssetDraft draft) {
        if (draft.getAssetTagId().isEmpty()) {
            return "Asset tag is required.";
        }
        if (draft.getDepartment().isEmpty()) {
            return "Department is required.";
        }
        if (draft.getDescription().isEmpty()) {
            return "Description is required.";
        }
        if (draft.getStatus().isEmpty()) {
            return "Status is required.";
        }
        if (draft.getSite().isEmpty()) {
            return "Site is required.";
        }
        if (draft.getCategory().isEmpty()) {
            return "Category is required.";
        }
        return null;
    }

    private OperationResult<ExceptionResolutionResult> confirmMovementInTransaction(MovementConfirmationRequest request) {
        long now = clock.nowUtcMillis();
        AssetEntity asset = database.assetDao().getByTag(request.getSessionId(), request.getAssetTagId());
        if (asset == null) {
            return OperationResult.fail(ErrorCode.NOT_FOUND, "Asset is no longer available.");
        }
        if (asset.auditStatus != AuditStatus.NOT_AUDITED
                || !safeEquals(asset.department, request.getExpectedPreviousDepartment())) {
            return OperationResult.fail(ErrorCode.INVALID_STATE, "Asset state changed before movement confirmation.");
        }
        int updated = database.assetDao().confirmMovedDepartment(
                request.getSessionId(),
                request.getAssetTagId(),
                request.getExpectedPreviousDepartment(),
                request.getSelectedDepartment(),
                request.getExpectedPreviousDepartment(),
                AuditStatus.NOT_AUDITED,
                AuditStatus.AUDITED_MOVED_DEPARTMENT,
                now
        );
        if (updated != 1) {
            return OperationResult.fail(ErrorCode.INVALID_STATE, "Movement could not be applied.");
        }
        insertEvent(request.getSessionId(), request.getAssetTagId(), EventKind.MOVEMENT_CONFIRMED,
                ScanResultType.SUCCESS_MOVED_DEPARTMENT, request.getSelectedDepartment(),
                request.getExpectedPreviousDepartment(), now, cleanNote(request.getNote(), "Movement confirmed"));
        DepartmentAuditEntity department = database.departmentAuditDao()
                .getDepartment(request.getSessionId(), request.getSelectedDepartment());
        if (department == null) {
            return OperationResult.fail(ErrorCode.NOT_FOUND, "Selected department is not available.");
        }
        int remaining = database.assetDao().countByDepartmentAndStatus(
                request.getSessionId(), request.getSelectedDepartment(), AuditStatus.NOT_AUDITED);
        DepartmentAuditStatus status = statusFor(request.getSessionId(), request.getSelectedDepartment(), remaining,
                department.missingCount, department.movedInCount + 1, department.newAssetCount);
        updateDepartmentCounts(department, department.scannedCount + 1, department.missingCount,
                department.movedInCount + 1, department.newAssetCount, status, remaining, now);
        diagnosticService.logInfo("Exceptions", "MOVEMENT_CONFIRMED tag=" + safeTag(request.getAssetTagId())
                + " to=" + request.getSelectedDepartment());
        return ok("Movement confirmed.", request.getSessionId(), department.departmentName);
    }

    private OperationResult<ExceptionResolutionResult> createNewAssetInTransaction(NewAssetDraft draft) {
        long now = clock.nowUtcMillis();
        if (database.assetDao().getByTag(draft.getSessionId(), draft.getAssetTagId()) != null) {
            return OperationResult.fail(ErrorCode.DUPLICATE_ASSET_TAG, "Asset tag already exists in this session.");
        }
        DepartmentAuditEntity department = database.departmentAuditDao().getDepartment(draft.getSessionId(), draft.getDepartment());
        if (department == null) {
            return OperationResult.fail(ErrorCode.NOT_FOUND, "Department is not available.");
        }
        AssetEntity entity = new AssetEntity(
                draft.getSessionId(),
                draft.getAssetTagId(),
                draft.getDepartment(),
                null,
                draft.getDescription(),
                draft.getStatus(),
                draft.getSite(),
                draft.getLocation(),
                draft.getCategory(),
                draft.getSubCategory(),
                draft.getOwner(),
                draft.getPrimaryUser(),
                AuditStatus.NEW_ASSET_ADDED,
                true,
                now
        );
        entity.auditDetails = draft.getNotes();
        database.assetDao().insert(entity);
        insertEvent(draft.getSessionId(), draft.getAssetTagId(), EventKind.NEW_ASSET_CREATED,
                null, draft.getDepartment(), null, now, cleanNote(draft.getNotes(), "New asset created"));
        int remaining = database.assetDao().countByDepartmentAndStatus(
                draft.getSessionId(), draft.getDepartment(), AuditStatus.NOT_AUDITED);
        DepartmentAuditStatus status = statusFor(draft.getSessionId(), draft.getDepartment(), remaining,
                department.missingCount, department.movedInCount, department.newAssetCount + 1);
        updateDepartmentCounts(department, department.scannedCount, department.missingCount,
                department.movedInCount, department.newAssetCount + 1, status, remaining, now);
        diagnosticService.logInfo("Exceptions", "NEW_ASSET_CREATED tag=" + safeTag(draft.getAssetTagId())
                + " department=" + draft.getDepartment());
        return ok("New asset added.", draft.getSessionId(), draft.getDepartment());
    }

    private OperationResult<ExceptionResolutionResult> markMissingInTransaction(String sessionId, String departmentName,
                                                                                List<String> assetTagIds) {
        long now = clock.nowUtcMillis();
        DepartmentAuditEntity department = database.departmentAuditDao().getDepartment(sessionId, departmentName);
        if (department == null) {
            return OperationResult.fail(ErrorCode.NOT_FOUND, "Department is not available.");
        }
        for (String tag : assetTagIds) {
            int updated = database.assetDao().updateStatusIfCurrent(
                    sessionId, tag, departmentName, AuditStatus.NOT_AUDITED,
                    AuditStatus.MISSING_AFTER_DEPARTMENT_CLOSE, now);
            if (updated != 1) {
                return OperationResult.fail(ErrorCode.INVALID_STATE, "Remaining assets changed before missing confirmation.");
            }
            insertEvent(sessionId, tag, EventKind.ASSET_MARKED_MISSING, null,
                    departmentName, null, now, "Marked missing at department finish");
        }
        int missingCount = department.missingCount + assetTagIds.size();
        int remaining = database.assetDao().countByDepartmentAndStatus(sessionId, departmentName, AuditStatus.NOT_AUDITED);
        DepartmentAuditStatus status = statusFor(sessionId, departmentName, remaining,
                missingCount, department.movedInCount, department.newAssetCount);
        updateDepartmentCounts(department, department.scannedCount, missingCount,
                department.movedInCount, department.newAssetCount, status, remaining, now);
        diagnosticService.logInfo("Exceptions", "MISSING_MARKED department=" + departmentName
                + " count=" + assetTagIds.size());
        return ok("Missing assets marked.", sessionId, departmentName);
    }

    private OperationResult<ExceptionResolutionResult> skipAssetsInTransaction(SkipAssetsRequest request) {
        long now = clock.nowUtcMillis();
        DepartmentAuditEntity department = database.departmentAuditDao().getDepartment(
                request.getSessionId(), request.getDepartment());
        if (department == null) {
            return OperationResult.fail(ErrorCode.NOT_FOUND, "Department is not available.");
        }
        for (String tag : request.getAssetTagIds()) {
            int updated = database.assetDao().updateStatusIfCurrent(
                    request.getSessionId(), tag, request.getDepartment(), AuditStatus.NOT_AUDITED,
                    AuditStatus.SKIPPED_UNABLE_TO_VERIFY, now);
            if (updated != 1) {
                return OperationResult.fail(ErrorCode.INVALID_STATE, "Remaining assets changed before skip confirmation.");
            }
            insertEvent(request.getSessionId(), tag, EventKind.ASSET_SKIPPED, null,
                    request.getDepartment(), null, now, "Skipped: " + request.getReason());
        }
        int remaining = database.assetDao().countByDepartmentAndStatus(
                request.getSessionId(), request.getDepartment(), AuditStatus.NOT_AUDITED);
        DepartmentAuditStatus status = statusFor(request.getSessionId(), request.getDepartment(), remaining,
                department.missingCount, department.movedInCount, department.newAssetCount);
        updateDepartmentCounts(department, department.scannedCount, department.missingCount,
                department.movedInCount, department.newAssetCount, status, remaining, now);
        diagnosticService.logInfo("Exceptions", "SKIPPED_MARKED department=" + request.getDepartment()
                + " count=" + request.getAssetTagIds().size()
                + " reasonLength=" + request.getReason().length());
        return ok("Skipped assets saved.", request.getSessionId(), request.getDepartment());
    }

    private ExceptionCountSummary buildExceptionCounts(String sessionId, String department, DepartmentAuditEntity audit) {
        int skipped = database.assetDao().countByDepartmentAndStatus(
                sessionId, department, AuditStatus.SKIPPED_UNABLE_TO_VERIFY);
        int duplicate = database.auditEventDao().countByDepartmentAndResult(
                sessionId, department, ScanResultType.DUPLICATE_SCAN);
        int invalid = database.auditEventDao().countByDepartmentAndResult(
                sessionId, department, ScanResultType.INVALID_SCAN);
        return new ExceptionCountSummary(
                audit.movedInCount,
                audit.newAssetCount,
                audit.missingCount,
                skipped,
                duplicate,
                invalid
        );
    }

    private DepartmentAuditStatus statusFor(String sessionId, String department, int remaining,
                                            int missingCount, int movedInCount, int newAssetCount) {
        if (remaining > 0) {
            return DepartmentAuditStatus.IN_PROGRESS;
        }
        int skipped = database.assetDao().countByDepartmentAndStatus(
                sessionId, department, AuditStatus.SKIPPED_UNABLE_TO_VERIFY);
        return missingCount + movedInCount + newAssetCount + skipped > 0
                ? DepartmentAuditStatus.COMPLETE_WITH_EXCEPTIONS
                : DepartmentAuditStatus.COMPLETE;
    }

    private void updateDepartmentCounts(DepartmentAuditEntity department, int scannedCount, int missingCount,
                                        int movedInCount, int newAssetCount, DepartmentAuditStatus status,
                                        int remaining, long now) {
        Long completedAt = remaining == 0 ? now : null;
        int updated = database.departmentAuditDao().updateCounts(
                department.sessionId,
                department.departmentName,
                scannedCount,
                missingCount,
                movedInCount,
                newAssetCount,
                status,
                completedAt
        );
        if (updated != 1) {
            throw new IllegalStateException("Department update affected " + updated + " rows.");
        }
    }

    private OperationResult<ExceptionResolutionResult> ok(String message, String sessionId, String department) {
        return OperationResult.ok(new ExceptionResolutionResult(
                message,
                departmentSummaryService.getDepartmentAuditContext(sessionId, "", department)
        ));
    }

    private void insertEvent(String sessionId, String assetTagId, EventKind eventKind, ScanResultType resultType,
                             String selectedDepartment, String previousDepartment, long now, String notes) {
        database.auditEventDao().insert(new AuditEventEntity(
                UUID.randomUUID().toString(),
                sessionId,
                assetTagId,
                eventKind,
                resultType,
                selectedDepartment,
                previousDepartment,
                now,
                userIdentityProvider.getDisplayName(),
                deviceInfoProvider.getModel(),
                notes
        ));
    }

    private String cleanNote(String note, String fallback) {
        return note == null || note.trim().isEmpty() ? fallback : note.trim();
    }

    private String safeTag(String value) {
        if (value == null) {
            return "";
        }
        return value.length() <= 12 ? value : value.substring(0, 12) + "...";
    }

    private boolean safeEquals(String left, String right) {
        if (left == null) {
            return right == null;
        }
        return left.equals(right);
    }
}
