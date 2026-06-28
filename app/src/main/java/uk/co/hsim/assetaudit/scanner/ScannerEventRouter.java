package uk.co.hsim.assetaudit.scanner;

import uk.co.hsim.assetaudit.app.AppExecutors;
import uk.co.hsim.assetaudit.data.entity.AuditSessionEntity;
import uk.co.hsim.assetaudit.hardening.DiagnosticRedactor;
import uk.co.hsim.assetaudit.service.DepartmentAuditContext;
import uk.co.hsim.assetaudit.service.DepartmentSummaryService;
import uk.co.hsim.assetaudit.service.DiagnosticService;
import uk.co.hsim.assetaudit.service.ScanProcessingResult;
import uk.co.hsim.assetaudit.service.ScanProcessor;
import uk.co.hsim.assetaudit.service.ScanRequest;
import uk.co.hsim.assetaudit.util.clock.Clock;

public final class ScannerEventRouter {
    public interface ContextProvider {
        AuditSessionEntity getActiveSession();

        String getSelectedDepartment();

        boolean isAuditScanVisible();

        boolean isLiveScannerEnabled();
    }

    public interface Callback {
        void onScannerRouteResult(ScannerRouteResult result);
    }

    private final AppExecutors executors;
    private final ScanProcessor scanProcessor;
    private final DepartmentSummaryService departmentSummaryService;
    private final DiagnosticService diagnosticService;
    private final DiagnosticRedactor diagnosticRedactor;
    private final Clock clock;
    private final long debounceMs;
    private String lastData;
    private long lastAcceptedAtUtc;

    public ScannerEventRouter(AppExecutors executors, ScanProcessor scanProcessor,
                              DepartmentSummaryService departmentSummaryService,
                              DiagnosticService diagnosticService, Clock clock, long debounceMs) {
        this.executors = executors;
        this.scanProcessor = scanProcessor;
        this.departmentSummaryService = departmentSummaryService;
        this.diagnosticService = diagnosticService;
        this.diagnosticRedactor = new DiagnosticRedactor();
        this.clock = clock;
        this.debounceMs = debounceMs;
    }

    public void route(ScannerPayload payload, ContextProvider contextProvider, Callback callback) {
        AuditSessionEntity session = contextProvider.getActiveSession();
        String selectedDepartment = contextProvider.getSelectedDepartment();
        ScannerRouteResult ignored = validate(payload, session, selectedDepartment, contextProvider);
        if (ignored != null) {
            executors.diskIO().execute(() -> {
                diagnosticService.logWarning("Scanner", ignored.getMessage());
                executors.mainThread(() -> callback.onScannerRouteResult(ignored));
            });
            return;
        }
        remember(payload);
        executors.diskIO().execute(() -> {
            ScanProcessingResult result = scanProcessor.processScan(new ScanRequest(
                    session.sessionId,
                    selectedDepartment,
                    payload.getData(),
                    "DATAWEDGE"
            ));
            DepartmentAuditContext context = departmentSummaryService.getDepartmentAuditContext(
                    session.sessionId,
                    session.auditName,
                    selectedDepartment
            );
            diagnosticService.logInfo("Scanner", "DATAWEDGE_SCAN result=" + result.getResultType()
                    + " " + diagnosticRedactor.scannerPayloadSummary(payload)
                    + " label=" + payload.getSymbology());
            ScannerRouteResult routed = ScannerRouteResult.processed(payload, result, context);
            executors.mainThread(() -> callback.onScannerRouteResult(routed));
        });
    }

    private ScannerRouteResult validate(ScannerPayload payload, AuditSessionEntity session, String selectedDepartment,
                                        ContextProvider contextProvider) {
        if (payload == null) {
            return ScannerRouteResult.ignored("Scanner payload missing", null);
        }
        if (!contextProvider.isLiveScannerEnabled()) {
            return ScannerRouteResult.ignored("Live scanner disabled", payload);
        }
        if (!contextProvider.isAuditScanVisible()) {
            return ScannerRouteResult.ignored("Scanner ignored outside Audit Scan", payload);
        }
        if (session == null) {
            return ScannerRouteResult.ignored("Scanner ignored without active session", payload);
        }
        if (selectedDepartment == null || selectedDepartment.trim().isEmpty()) {
            return ScannerRouteResult.ignored("Scanner ignored without selected department", payload);
        }
        if (isDuplicateTransport(payload)) {
            return ScannerRouteResult.ignored("Duplicate scanner transport ignored", payload);
        }
        return null;
    }

    private boolean isDuplicateTransport(ScannerPayload payload) {
        long now = clock.nowUtcMillis();
        return lastData != null
                && lastData.equals(payload.getData())
                && now - lastAcceptedAtUtc <= debounceMs;
    }

    private void remember(ScannerPayload payload) {
        lastData = payload.getData();
        lastAcceptedAtUtc = clock.nowUtcMillis();
    }

}
