package uk.co.hsim.assetaudit.importfile;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public final class HeaderDetectionResult {
    private final boolean found;
    private final int sourceRowNumber;
    private final Map<String, Integer> normalizedHeaderIndexes;
    private final Map<String, String> normalizedToDisplayName;

    private HeaderDetectionResult(boolean found, int sourceRowNumber,
                                  Map<String, Integer> normalizedHeaderIndexes,
                                  Map<String, String> normalizedToDisplayName) {
        this.found = found;
        this.sourceRowNumber = sourceRowNumber;
        this.normalizedHeaderIndexes = Collections.unmodifiableMap(new LinkedHashMap<>(normalizedHeaderIndexes));
        this.normalizedToDisplayName = Collections.unmodifiableMap(new LinkedHashMap<>(normalizedToDisplayName));
    }

    public static HeaderDetectionResult found(int sourceRowNumber, Map<String, Integer> indexes,
                                              Map<String, String> displayNames) {
        return new HeaderDetectionResult(true, sourceRowNumber, indexes, displayNames);
    }

    public static HeaderDetectionResult notFound() {
        return new HeaderDetectionResult(false, -1, Collections.emptyMap(), Collections.emptyMap());
    }

    public boolean isFound() {
        return found;
    }

    public int getSourceRowNumber() {
        return sourceRowNumber;
    }

    public Map<String, Integer> getNormalizedHeaderIndexes() {
        return normalizedHeaderIndexes;
    }

    public Map<String, String> getNormalizedToDisplayName() {
        return normalizedToDisplayName;
    }
}
