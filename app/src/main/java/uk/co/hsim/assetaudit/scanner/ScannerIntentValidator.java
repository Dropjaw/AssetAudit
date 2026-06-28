package uk.co.hsim.assetaudit.scanner;

import android.content.Intent;
import android.os.Bundle;

import java.util.HashSet;
import java.util.Set;

import uk.co.hsim.assetaudit.domain.rules.BarcodeNormalizer;

public final class ScannerIntentValidator {
    public static final int MAX_DATA_STRING_LENGTH = 128;
    private final Set<String> allowedExtras = new HashSet<>();

    public ScannerIntentValidator() {
        allowedExtras.add(DataWedgeConstants.EXTRA_DATA_STRING);
        allowedExtras.add(DataWedgeConstants.EXTRA_LABEL_TYPE);
        allowedExtras.add(DataWedgeConstants.EXTRA_SOURCE);
        allowedExtras.add(DataWedgeConstants.EXTRA_DECODE_DATA);
    }

    public ScannerPayloadValidationResult validate(Intent intent) {
        if (intent == null) {
            return ScannerPayloadValidationResult.invalid("Missing intent");
        }
        if (!DataWedgeConstants.ACTION_SCAN.equals(intent.getAction())) {
            return ScannerPayloadValidationResult.invalid("Unexpected action");
        }
        if (!intent.hasCategory(DataWedgeConstants.CATEGORY_SCAN)) {
            return ScannerPayloadValidationResult.invalid("Missing scanner category");
        }
        Bundle extras = intent.getExtras();
        if (extras == null) {
            return ScannerPayloadValidationResult.invalid("Missing extras");
        }
        for (String key : extras.keySet()) {
            if (!allowedExtras.contains(key)) {
                return ScannerPayloadValidationResult.invalid("Unexpected scanner extra");
            }
        }
        String data = intent.getStringExtra(DataWedgeConstants.EXTRA_DATA_STRING);
        if (!BarcodeNormalizer.isValidBasicAssetTag(data)) {
            return ScannerPayloadValidationResult.invalid("Missing barcode data");
        }
        if (BarcodeNormalizer.normalizeAssetTag(data).length() > MAX_DATA_STRING_LENGTH) {
            return ScannerPayloadValidationResult.invalid("Barcode data too long");
        }
        return ScannerPayloadValidationResult.valid();
    }
}
