package uk.co.hsim.assetaudit.service;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import uk.co.hsim.assetaudit.data.db.AuditDatabase;
import uk.co.hsim.assetaudit.data.entity.AuditEventEntity;
import uk.co.hsim.assetaudit.data.entity.AuditSessionEntity;
import uk.co.hsim.assetaudit.domain.enums.AuditStatus;
import uk.co.hsim.assetaudit.domain.enums.EventKind;
import uk.co.hsim.assetaudit.domain.enums.SessionStatus;
import uk.co.hsim.assetaudit.domain.results.ErrorCode;
import uk.co.hsim.assetaudit.domain.results.OperationResult;
import uk.co.hsim.assetaudit.hardening.DiagnosticRedactor;
import uk.co.hsim.assetaudit.util.clock.Clock;
import uk.co.hsim.assetaudit.util.device.DeviceInfoProvider;
import uk.co.hsim.assetaudit.util.identity.UserIdentityProvider;

public final class AuditCompletionService {
    private final AuditDatabase database;
    private final DiagnosticService diagnosticService;
    private final Clock clock;
    private final UserIdentityProvider userIdentityProvider;
    private final DeviceInfoProvider deviceInfoProvider;
    private final DiagnosticRedactor redactor;

    public AuditCompletionService(AuditDatabase database, DiagnosticService diagnosticService, Clock clock,
                                  UserIdentityProvider userIdentityProvider,
                                  DeviceInfoProvider deviceInfoProvider) {
        this.database = database;
        this.diagnosticService = diagnosticService;
        this.clock = clock;
        this.userIdentityProvider = userIdentityProvider;
        this.deviceInfoProvider = deviceInfoProvider;
        this.redactor = new DiagnosticRedactor();
    }

    public OperationResult<AuditCompletionState> getCompletionState(String sessionId) {
        AuditSessionEntity session = database.auditSessionDao().getById(sessionId);
        if (session == null) {
            return OperationResult.fail(ErrorCode.NOT_FOUND, "Audit session was not found.");
        }
        return OperationResult.ok(buildState(session));
    }

    public OperationResult<AuditCompletionResult> tryCompleteAfterFinalExport(String sessionId,
                                                                              String exportRunId,
                                                                              String packageId) {
        AtomicReference<AuditCompletionResult> resultRef = new AtomicReference<>();
        try {
            database.runInTransaction(() -> resultRef.set(completeInTransaction(sessionId, exportRunId, packageId)));
            AuditCompletionResult result = resultRef.get();
            if (result.isChanged()) {
                diagnosticService.logInfo("Lifecycle", "AUDIT_COMPLETED session=" + sessionId
                        + " package=" + packageId);
            } else if (result.getFinalStatus() == SessionStatus.ACTIVE) {
                diagnosticService.logInfo("Lifecycle", "AUDIT_COMPLETION_BLOCKED session=" + sessionId
                        + " blockers=" + result.getState().getBlockers().size());
            }
            return OperationResult.ok(result);
        } catch (RuntimeException e) {
            diagnosticService.logError("Lifecycle", "AUDIT_COMPLETION_FAILED session=" + sessionId, e);
            return OperationResult.fail(ErrorCode.DATABASE_ERROR, "Audit completion failed.");
        }
    }

    public OperationResult<AuditCompletionResult> forceCloseCurrentAudit(ForceCloseAuditRequest request) {
        if (request == null || request.getSessionId() == null || request.getSessionId().trim().isEmpty()) {
            return OperationResult.fail(ErrorCode.VALIDATION_FAILED, "No active audit session is available.");
        }
        if (!request.isUserConfirmed()) {
            return OperationResult.fail(ErrorCode.VALIDATION_FAILED, "Force close confirmation is required.");
        }
        String reason = sanitizeReason(request.getReason());
        if (reason.isEmpty()) {
            return OperationResult.fail(ErrorCode.VALIDATION_FAILED, "Force close reason is required.");
        }
        AtomicReference<AuditCompletionResult> resultRef = new AtomicReference<>();
        try {
            database.runInTransaction(() -> resultRef.set(forceCloseInTransaction(request.getSessionId(), reason)));
            AuditCompletionResult result = resultRef.get();
            if (result.isChanged()) {
                diagnosticService.logWarning("Lifecycle", "AUDIT_FORCE_CLOSED session=" + request.getSessionId()
                        + " incompleteDepartments=" + result.getState().getIncompleteDepartments()
                        + " remainingAssets=" + result.getState().getRemainingAssets()
                        + " reasonLength=" + reason.length());
            }
            return OperationResult.ok(result);
        } catch (RuntimeException e) {
            diagnosticService.logError("Lifecycle", "AUDIT_FORCE_CLOSE_FAILED session=" + request.getSessionId(), e);
            return OperationResult.fail(ErrorCode.DATABASE_ERROR, "Force close failed.");
        }
    }

    private AuditCompletionResult completeInTransaction(String sessionId, String exportRunId, String packageId) {
        AuditSessionEntity session = database.auditSessionDao().getById(sessionId);
        if (session == null) {
            return new AuditCompletionResult(sessionId, null, false, "Audit session was not found.", null);
        }
        AuditCompletionState state = buildState(session);
        if (session.status == SessionStatus.COMPLETED) {
            return new AuditCompletionResult(sessionId, SessionStatus.COMPLETED, false,
                    "Audit is already completed.", state);
        }
        if (session.status != SessionStatus.ACTIVE) {
            return new AuditCompletionResult(sessionId, session.status, false,
                    "Audit is not active and cannot be completed.", state);
        }
        if (state.hasSessionCompletedEvent()) {
            database.auditSessionDao().updateStatusIfCurrent(sessionId, SessionStatus.ACTIVE,
                    SessionStatus.COMPLETED, session.completedAtUtc == null ? clock.nowUtcMillis() : session.completedAtUtc);
            return new AuditCompletionResult(sessionId, SessionStatus.COMPLETED, false,
                    "Audit completion event already exists.", buildState(database.auditSessionDao().getById(sessionId)));
        }
        if (!state.isEligibleForNormalCompletion()) {
            return new AuditCompletionResult(sessionId, SessionStatus.ACTIVE, false,
                    "Audit remains active: " + firstBlocker(state), state);
        }
        long now = clock.nowUtcMillis();
        int updated = database.auditSessionDao().updateStatusIfCurrent(sessionId, SessionStatus.ACTIVE,
                SessionStatus.COMPLETED, now);
        if (updated != 1) {
            return new AuditCompletionResult(sessionId, database.auditSessionDao().getById(sessionId).status, false,
                    "Audit lifecycle changed before completion.", buildState(database.auditSessionDao().getById(sessionId)));
        }
        insertLifecycleEvent(sessionId, EventKind.SESSION_COMPLETED,
                "Audit completed after final export package=" + nullToEmpty(packageId)
                        + " exportRun=" + nullToEmpty(exportRunId));
        return new AuditCompletionResult(sessionId, SessionStatus.COMPLETED, true,
                "Audit completed.", buildState(database.auditSessionDao().getById(sessionId)));
    }

    private AuditCompletionResult forceCloseInTransaction(String sessionId, String reason) {
        AuditSessionEntity session = database.auditSessionDao().getById(sessionId);
        if (session == null) {
            return new AuditCompletionResult(sessionId, null, false, "Audit session was not found.", null);
        }
        AuditCompletionState state = buildState(session);
        if (session.status != SessionStatus.ACTIVE) {
            return new AuditCompletionResult(sessionId, session.status, false,
                    "Only an active audit can be force closed.", state);
        }
        if (database.auditEventDao().countBySessionAndKind(sessionId, EventKind.SESSION_FORCE_CLOSED) > 0) {
            return new AuditCompletionResult(sessionId, SessionStatus.FORCE_CLOSED, false,
                    "Audit force close event already exists.", state);
        }
        long now = clock.nowUtcMillis();
        int updated = database.auditSessionDao().updateStatusIfCurrent(sessionId, SessionStatus.ACTIVE,
                SessionStatus.FORCE_CLOSED, now);
        if (updated != 1) {
            return new AuditCompletionResult(sessionId, database.auditSessionDao().getById(sessionId).status, false,
                    "Audit lifecycle changed before force close.", buildState(database.auditSessionDao().getById(sessionId)));
        }
        insertLifecycleEvent(sessionId, EventKind.SESSION_FORCE_CLOSED,
                "Audit force closed. Reason length=" + reason.length() + ". Reason: " + reason);
        return new AuditCompletionResult(sessionId, SessionStatus.FORCE_CLOSED, true,
                "Audit force closed.", buildState(database.auditSessionDao().getById(sessionId)));
    }

    private AuditCompletionState buildState(AuditSessionEntity session) {
        String sessionId = session.sessionId;
        int totalDepartments = database.departmentAuditDao().countDepartments(sessionId);
        int completeDepartments = database.departmentAuditDao().countCompleteDepartments(sessionId);
        int incompleteDepartments = database.departmentAuditDao().countIncompleteDepartments(sessionId);
        int remainingAssets = database.assetDao().countByStatus(sessionId, AuditStatus.NOT_AUDITED);
        boolean hasFinalExport = database.exportRunDao().countFinalExports(sessionId) > 0;
        boolean hasSessionExportedEvent = database.auditEventDao()
                .countBySessionAndKind(sessionId, EventKind.SESSION_EXPORTED) > 0;
        boolean hasSessionCompletedEvent = database.auditEventDao()
                .countBySessionAndKind(sessionId, EventKind.SESSION_COMPLETED) > 0;
        List<String> blockers = new ArrayList<>();
        if (session.status != SessionStatus.ACTIVE) {
            blockers.add("Audit status is " + session.status.name() + ".");
        }
        if (totalDepartments == 0) {
            blockers.add("No department summaries exist.");
        }
        if (incompleteDepartments > 0) {
            blockers.add(incompleteDepartments + " departments are not complete.");
        }
        if (remainingAssets > 0) {
            blockers.add(remainingAssets + " assets remain not audited.");
        }
        if (!hasFinalExport) {
            blockers.add("Final export is missing.");
        }
        if (!hasSessionExportedEvent) {
            blockers.add("Export evidence event is missing.");
        }
        boolean eligible = session.status == SessionStatus.ACTIVE
                && totalDepartments > 0
                && incompleteDepartments == 0
                && remainingAssets == 0
                && hasFinalExport
                && hasSessionExportedEvent
                && !hasSessionCompletedEvent;
        return new AuditCompletionState(sessionId, session.status, totalDepartments, completeDepartments,
                incompleteDepartments, remainingAssets, hasFinalExport, hasSessionExportedEvent,
                hasSessionCompletedEvent, eligible, blockers);
    }

    private void insertLifecycleEvent(String sessionId, EventKind eventKind, String notes) {
        database.auditEventDao().insert(new AuditEventEntity(
                UUID.randomUUID().toString(),
                sessionId,
                null,
                eventKind,
                null,
                null,
                null,
                clock.nowUtcMillis(),
                userIdentityProvider.getDisplayName(),
                deviceInfoProvider.getModel(),
                redactor.redactMessage(notes)
        ));
    }

    private String firstBlocker(AuditCompletionState state) {
        return state.getBlockers().isEmpty() ? "completion requirements are not satisfied." : state.getBlockers().get(0);
    }

    private String sanitizeReason(String reason) {
        return reason == null ? "" : reason.trim().replaceAll("[\\p{Cntrl}&&[^\r\n\t]]", "?");
    }

    private String nullToEmpty(String value) {
        return value == null ? "" : value;
    }
}
