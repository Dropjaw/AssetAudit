package uk.co.hsim.assetaudit.scanner;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import uk.co.hsim.assetaudit.util.clock.Clock;

public final class DataWedgeScanReceiver extends BroadcastReceiver {
    public interface Listener {
        void onScannerPayload(ScannerPayload payload);

        void onScannerPayloadIgnored(String reason);
    }

    private final ScannerPayloadParser parser;
    private final Clock clock;
    private Listener listener;

    public DataWedgeScanReceiver(ScannerPayloadParser parser, Clock clock) {
        this.parser = parser;
        this.clock = clock;
    }

    public void setListener(Listener listener) {
        this.listener = listener;
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        ScannerPayloadParser.ParseResult result = parser.parse(intent, clock.nowUtcMillis());
        if (listener == null) {
            return;
        }
        if (result.isAccepted()) {
            listener.onScannerPayload(result.getPayload());
        } else {
            listener.onScannerPayloadIgnored(result.getReason());
        }
    }
}
