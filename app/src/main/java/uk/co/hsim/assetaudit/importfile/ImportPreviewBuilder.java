package uk.co.hsim.assetaudit.importfile;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class ImportPreviewBuilder {
    public ImportPreview build(DocumentReference documentReference, ParsedAssetFile parsedFile,
                               HeaderDetectionResult headerDetectionResult,
                               ImportValidationResult validationResult,
                               ImportSettings settings) {
        Map<String, Integer> departmentCounts = new LinkedHashMap<>();
        Map<String, Integer> tagCounts = new HashMap<>();
        int leadingZeroTags = 0;
        int alphanumericTags = 0;
        for (AssetImportRow row : validationResult.getAcceptedRows()) {
            String department = normalizeDepartment(row.getDepartment(), settings.getUnassignedDepartmentLabel());
            departmentCounts.put(department, departmentCounts.getOrDefault(department, 0) + 1);
            String tag = row.getAssetTagId();
            if (tag.matches("0[0-9]+")) {
                leadingZeroTags++;
            }
            if (tag.matches(".*[A-Za-z].*")) {
                alphanumericTags++;
            }
            tagCounts.put(tag, tagCounts.getOrDefault(tag, 0) + 1);
        }
        int duplicateTags = 0;
        for (Integer count : tagCounts.values()) {
            if (count > 1) {
                duplicateTags++;
            }
        }
        return new ImportPreview(documentReference, parsedFile, headerDetectionResult, validationResult,
                departmentCounts, leadingZeroTags, alphanumericTags, duplicateTags);
    }

    public static String normalizeDepartment(String value, String unassignedLabel) {
        String trimmed = value == null ? "" : value.trim();
        return trimmed.isEmpty() ? unassignedLabel : trimmed;
    }
}
