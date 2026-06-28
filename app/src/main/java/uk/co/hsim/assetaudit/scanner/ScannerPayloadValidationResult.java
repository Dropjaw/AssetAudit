package uk.co.hsim.assetaudit.scanner;

public final class ScannerPayloadValidationResult {
    private final boolean valid;
    private final String reason;

    private ScannerPayloadValidationResult(boolean valid, String reason) {
        this.valid = valid;
        this.reason = reason == null ? "" : reason;
    }

    public static ScannerPayloadValidationResult valid() {
        return new ScannerPayloadValidationResult(true, "");
    }

    public static ScannerPayloadValidationResult invalid(String reason) {
        return new ScannerPayloadValidationResult(false, reason);
    }

    public boolean isValid() {
        return valid;
    }

    public String getReason() {
        return reason;
    }
}
