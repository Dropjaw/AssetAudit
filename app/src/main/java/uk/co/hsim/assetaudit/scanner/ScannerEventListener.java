package uk.co.hsim.assetaudit.scanner;

public interface ScannerEventListener {
    void onScannerPayload(ScannerPayload payload);
}
