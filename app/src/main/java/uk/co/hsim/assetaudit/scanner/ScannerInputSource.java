package uk.co.hsim.assetaudit.scanner;

public interface ScannerInputSource {
    void setListener(ScannerEventListener listener);

    void start();

    void stop();
}
