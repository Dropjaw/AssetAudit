package uk.co.hsim.assetaudit.service;

public final class ScanRequest {
    private final String sessionId;
    private final String selectedDepartment;
    private final String barcodeRaw;
    private final String source;

    public ScanRequest(String sessionId, String selectedDepartment, String barcodeRaw, String source) {
        this.sessionId = sessionId;
        this.selectedDepartment = selectedDepartment;
        this.barcodeRaw = barcodeRaw;
        this.source = source;
    }

    public String getSessionId() {
        return sessionId;
    }

    public String getSelectedDepartment() {
        return selectedDepartment;
    }

    public String getBarcodeRaw() {
        return barcodeRaw;
    }

    public String getSource() {
        return source;
    }
}
