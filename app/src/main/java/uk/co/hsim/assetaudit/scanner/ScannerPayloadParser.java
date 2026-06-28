package uk.co.hsim.assetaudit.scanner;

import android.content.Intent;

import java.util.Map;

import uk.co.hsim.assetaudit.domain.rules.BarcodeNormalizer;

public final class ScannerPayloadParser {
    private final ScannerIntentValidator intentValidator;

    public ScannerPayloadParser() {
        this(new ScannerIntentValidator());
    }

    public ScannerPayloadParser(ScannerIntentValidator intentValidator) {
        this.intentValidator = intentValidator;
    }

    public ParseResult parse(Intent intent, long receivedAtUtc) {
        ScannerPayloadValidationResult validation = intentValidator.validate(intent);
        if (!validation.isValid()) {
            return ParseResult.ignored(validation.getReason());
        }
        String data = intent.getStringExtra(DataWedgeConstants.EXTRA_DATA_STRING);
        String labelType = intent.getStringExtra(DataWedgeConstants.EXTRA_LABEL_TYPE);
        String source = intent.getStringExtra(DataWedgeConstants.EXTRA_SOURCE);
        return parse(DataWedgeConstants.ACTION_SCAN, data, labelType, source, receivedAtUtc);
    }

    public ParseResult parse(String action, Map<String, String> extras, long receivedAtUtc) {
        if (extras == null) {
            return ParseResult.ignored("Missing extras");
        }
        return parse(
                action,
                extras.get(DataWedgeConstants.EXTRA_DATA_STRING),
                extras.get(DataWedgeConstants.EXTRA_LABEL_TYPE),
                extras.get(DataWedgeConstants.EXTRA_SOURCE),
                receivedAtUtc
        );
    }

    private ParseResult parse(String action, String data, String labelType, String source, long receivedAtUtc) {
        if (!DataWedgeConstants.ACTION_SCAN.equals(action)) {
            return ParseResult.ignored("Unexpected action");
        }
        if (!BarcodeNormalizer.isValidBasicAssetTag(data)) {
            return ParseResult.ignored("Missing barcode data");
        }
        return ParseResult.accepted(new ScannerPayload(
                data,
                labelType == null ? "" : labelType,
                receivedAtUtc,
                source == null ? "DATAWEDGE" : source
        ));
    }

    public static final class ParseResult {
        private final boolean accepted;
        private final ScannerPayload payload;
        private final String reason;

        private ParseResult(boolean accepted, ScannerPayload payload, String reason) {
            this.accepted = accepted;
            this.payload = payload;
            this.reason = reason;
        }

        public static ParseResult accepted(ScannerPayload payload) {
            return new ParseResult(true, payload, "");
        }

        public static ParseResult ignored(String reason) {
            return new ParseResult(false, null, reason);
        }

        public boolean isAccepted() {
            return accepted;
        }

        public ScannerPayload getPayload() {
            return payload;
        }

        public String getReason() {
            return reason;
        }
    }
}
