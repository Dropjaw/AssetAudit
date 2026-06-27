package uk.co.hsim.assetaudit.scanner;

import uk.co.hsim.assetaudit.util.clock.Clock;

public final class ManualScannerInputSource implements ScannerInputSource {
    private final Clock clock;
    private ScannerEventListener listener;

    public ManualScannerInputSource(Clock clock) {
        this.clock = clock;
    }

    @Override
    public void setListener(ScannerEventListener listener) {
        this.listener = listener;
    }

    @Override
    public void start() {
    }

    @Override
    public void stop() {
    }

    public void submitManualValue(String value) {
        if (listener != null) {
            listener.onScannerPayload(new ScannerPayload(value, "MANUAL", clock.nowUtcMillis(), "manual"));
        }
    }
}
