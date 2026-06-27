package uk.co.hsim.assetaudit.domain.rules;

public final class BarcodeNormalizer {
    private static final int MAX_ASSET_TAG_LENGTH = 128;

    private BarcodeNormalizer() {
    }

    public static String normalizeAssetTag(String raw) {
        if (raw == null) {
            return "";
        }
        return raw.trim();
    }

    public static boolean isValidBasicAssetTag(String value) {
        String normalized = normalizeAssetTag(value);
        return !normalized.isEmpty() && normalized.length() <= MAX_ASSET_TAG_LENGTH;
    }
}
