package uk.co.hsim.assetaudit.importfile;

import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public final class HeaderDetector {
    public static final String ASSET_TAG_ID = "asset tag id";
    public static final String DEPARTMENT = "department";
    public static final String DESCRIPTION = "description";
    public static final String STATUS = "status";
    public static final String BRAND = "brand";
    public static final String MODEL = "model";
    public static final String SITE = "site";
    public static final String LOCATION = "location";
    public static final String CATEGORY = "category";
    public static final String SUB_CATEGORY = "sub category";
    public static final String PRIMARY_USER = "primary user";
    public static final String OWNER = "owner";
    public static final String AUDIT_DETAILS = "audit details";

    private static final Set<String> REQUIRED_ANCHORS = new HashSet<>(Arrays.asList(ASSET_TAG_ID, DEPARTMENT));
    private static final Set<String> KNOWN_HEADERS = new HashSet<>(Arrays.asList(
            ASSET_TAG_ID, DEPARTMENT, DESCRIPTION, STATUS, BRAND, MODEL, SITE, LOCATION,
            CATEGORY, SUB_CATEGORY, PRIMARY_USER, OWNER, AUDIT_DETAILS
    ));

    public HeaderDetectionResult detect(List<RawAssetRow> candidateRows) {
        int limit = Math.min(20, candidateRows.size());
        for (int i = 0; i < limit; i++) {
            RawAssetRow row = candidateRows.get(i);
            Map<String, Integer> indexes = new LinkedHashMap<>();
            Map<String, String> displayNames = new LinkedHashMap<>();
            for (int cellIndex = 0; cellIndex < row.getCells().size(); cellIndex++) {
                String display = clean(row.getCells().get(cellIndex));
                if (display.isEmpty()) {
                    continue;
                }
                String normalized = normalizeHeader(display);
                if (!indexes.containsKey(normalized)) {
                    indexes.put(normalized, cellIndex);
                    displayNames.put(normalized, display);
                }
            }
            if (indexes.keySet().containsAll(REQUIRED_ANCHORS) && countKnownHeaders(indexes.keySet()) >= 5) {
                return HeaderDetectionResult.found(row.getSourceRowNumber(), indexes, displayNames);
            }
        }
        return HeaderDetectionResult.notFound();
    }

    public static String normalizeHeader(String value) {
        return clean(value).replaceAll("\\s+", " ").toLowerCase(Locale.ROOT);
    }

    private static int countKnownHeaders(Set<String> headers) {
        int count = 0;
        for (String header : headers) {
            if (KNOWN_HEADERS.contains(header)) {
                count++;
            }
        }
        return count;
    }

    private static String clean(String value) {
        return value == null ? "" : value.trim();
    }
}
