package uk.co.hsim.assetaudit.scanner;

import uk.co.hsim.assetaudit.service.DepartmentAuditContext;
import uk.co.hsim.assetaudit.service.ScanProcessingResult;

public final class ScannerRouteResult {
    private final boolean processed;
    private final String message;
    private final ScannerPayload payload;
    private final ScanProcessingResult scanResult;
    private final DepartmentAuditContext departmentContext;

    private ScannerRouteResult(boolean processed, String message, ScannerPayload payload,
                               ScanProcessingResult scanResult, DepartmentAuditContext departmentContext) {
        this.processed = processed;
        this.message = message;
        this.payload = payload;
        this.scanResult = scanResult;
        this.departmentContext = departmentContext;
    }

    public static ScannerRouteResult ignored(String message, ScannerPayload payload) {
        return new ScannerRouteResult(false, message, payload, null, null);
    }

    public static ScannerRouteResult processed(ScannerPayload payload, ScanProcessingResult scanResult,
                                               DepartmentAuditContext departmentContext) {
        return new ScannerRouteResult(true, scanResult.getMessage(), payload, scanResult, departmentContext);
    }

    public boolean isProcessed() {
        return processed;
    }

    public String getMessage() {
        return message;
    }

    public ScannerPayload getPayload() {
        return payload;
    }

    public ScanProcessingResult getScanResult() {
        return scanResult;
    }

    public DepartmentAuditContext getDepartmentContext() {
        return departmentContext;
    }
}
