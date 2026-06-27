package uk.co.hsim.assetaudit.importfile;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class ImportPreview {
    private final DocumentReference documentReference;
    private final ParsedAssetFile parsedFile;
    private final HeaderDetectionResult headerDetectionResult;
    private final ImportValidationResult validationResult;
    private final Map<String, Integer> departmentCounts;
    private final int leadingZeroTagCount;
    private final int alphanumericTagCount;
    private final int duplicateTagCount;

    public ImportPreview(DocumentReference documentReference, ParsedAssetFile parsedFile,
                         HeaderDetectionResult headerDetectionResult,
                         ImportValidationResult validationResult,
                         Map<String, Integer> departmentCounts,
                         int leadingZeroTagCount, int alphanumericTagCount, int duplicateTagCount) {
        this.documentReference = documentReference;
        this.parsedFile = parsedFile;
        this.headerDetectionResult = headerDetectionResult;
        this.validationResult = validationResult;
        this.departmentCounts = Collections.unmodifiableMap(new LinkedHashMap<>(departmentCounts));
        this.leadingZeroTagCount = leadingZeroTagCount;
        this.alphanumericTagCount = alphanumericTagCount;
        this.duplicateTagCount = duplicateTagCount;
    }

    public DocumentReference getDocumentReference() {
        return documentReference;
    }

    public ParsedAssetFile getParsedFile() {
        return parsedFile;
    }

    public HeaderDetectionResult getHeaderDetectionResult() {
        return headerDetectionResult;
    }

    public ImportValidationResult getValidationResult() {
        return validationResult;
    }

    public Map<String, Integer> getDepartmentCounts() {
        return departmentCounts;
    }

    public int getLeadingZeroTagCount() {
        return leadingZeroTagCount;
    }

    public int getAlphanumericTagCount() {
        return alphanumericTagCount;
    }

    public int getDuplicateTagCount() {
        return duplicateTagCount;
    }

    public List<ImportIssue> firstIssues(int count) {
        List<ImportIssue> issues = validationResult.getIssues();
        return Collections.unmodifiableList(new ArrayList<>(issues.subList(0, Math.min(count, issues.size()))));
    }
}
