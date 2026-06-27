package uk.co.hsim.assetaudit.importfile;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import uk.co.hsim.assetaudit.domain.enums.ImportIssueSeverity;

public final class AssetImportValidator {
    private static final int MAX_ASSET_TAG_LENGTH = 128;
    private static final Set<String> REQUIRED_HEADERS = new LinkedHashSet<>();

    static {
        REQUIRED_HEADERS.add(HeaderDetector.ASSET_TAG_ID);
        REQUIRED_HEADERS.add(HeaderDetector.DEPARTMENT);
        REQUIRED_HEADERS.add(HeaderDetector.DESCRIPTION);
        REQUIRED_HEADERS.add(HeaderDetector.STATUS);
        REQUIRED_HEADERS.add(HeaderDetector.SITE);
        REQUIRED_HEADERS.add(HeaderDetector.LOCATION);
        REQUIRED_HEADERS.add(HeaderDetector.CATEGORY);
    }

    public ImportValidationResult validate(ParsedAssetFile parsedFile, List<AssetImportRow> rows,
                                           HeaderDetectionResult headerDetection, ImportSettings settings) {
        List<ImportIssue> issues = new ArrayList<>();
        if (!headerDetection.isFound()) {
            issues.add(issue(null, null, ImportIssueSeverity.FATAL, ImportIssueCode.HEADER_ROW_NOT_FOUND,
                    "Could not find a header row containing Asset Tag ID and Department.", null));
            return result(issues, new ArrayList<>());
        }

        validateHeaders(headerDetection, issues);
        Map<String, List<Integer>> duplicateTracker = new HashMap<>();
        List<AssetImportRow> acceptedRows = new ArrayList<>();
        for (AssetImportRow row : rows) {
            boolean rowFatal = validateRow(row, settings, issues);
            if (!row.getAssetTagId().isEmpty()) {
                duplicateTracker.computeIfAbsent(row.getAssetTagId(), key -> new ArrayList<>()).add(row.getSourceRowNumber());
            }
            if (!rowFatal) {
                acceptedRows.add(row);
            }
        }
        for (Map.Entry<String, List<Integer>> entry : duplicateTracker.entrySet()) {
            if (entry.getValue().size() > 1) {
                issues.add(issue(null, "Asset Tag ID", ImportIssueSeverity.FATAL, ImportIssueCode.ASSET_TAG_DUPLICATE,
                        "Duplicate asset tag " + entry.getKey() + " appears on rows " + entry.getValue() + ".", entry.getKey()));
            }
        }
        return result(issues, acceptedRows);
    }

    private void validateHeaders(HeaderDetectionResult headerDetection, List<ImportIssue> issues) {
        Set<String> seenDisplay = new HashSet<>();
        for (String display : headerDetection.getNormalizedToDisplayName().values()) {
            String normalized = HeaderDetector.normalizeHeader(display);
            if (!seenDisplay.add(normalized)) {
                issues.add(issue(headerDetection.getSourceRowNumber(), display, ImportIssueSeverity.FATAL,
                        ImportIssueCode.DUPLICATE_COLUMN, "Duplicate source column: " + display, display));
            }
        }
        for (String required : REQUIRED_HEADERS) {
            if (!headerDetection.getNormalizedHeaderIndexes().containsKey(required)) {
                issues.add(issue(headerDetection.getSourceRowNumber(), required, ImportIssueSeverity.FATAL,
                        ImportIssueCode.REQUIRED_COLUMN_MISSING, "Required column missing: " + required, required));
            }
        }
    }

    private boolean validateRow(AssetImportRow row, ImportSettings settings, List<ImportIssue> issues) {
        boolean fatal = false;
        fatal |= require(row, row.getAssetTagId(), "Asset Tag ID", ImportIssueCode.ASSET_TAG_BLANK, issues);
        if (row.getAssetTagId().length() > MAX_ASSET_TAG_LENGTH) {
            issues.add(issue(row.getSourceRowNumber(), "Asset Tag ID", ImportIssueSeverity.FATAL,
                    ImportIssueCode.ASSET_TAG_TOO_LONG, "Asset Tag ID is longer than 128 characters.", row.getAssetTagId()));
            fatal = true;
        }
        if (row.getDepartment().isEmpty()) {
            issues.add(issue(row.getSourceRowNumber(), "Department", ImportIssueSeverity.WARNING,
                    ImportIssueCode.DEPARTMENT_BLANK, "Department is blank and will be imported as "
                    + settings.getUnassignedDepartmentLabel() + ".", null));
        }
        fatal |= require(row, row.getDescription(), "Description", ImportIssueCode.DESCRIPTION_BLANK, issues);
        fatal |= require(row, row.getStatus(), "Status", ImportIssueCode.STATUS_BLANK, issues);
        fatal |= require(row, row.getSite(), "Site", ImportIssueCode.SITE_BLANK, issues);
        fatal |= require(row, row.getLocation(), "Location", ImportIssueCode.LOCATION_BLANK, issues);
        fatal |= require(row, row.getCategory(), "Category", ImportIssueCode.CATEGORY_BLANK, issues);
        if (row.getSubCategory().isEmpty()) {
            issues.add(issue(row.getSourceRowNumber(), "Sub Category", ImportIssueSeverity.WARNING,
                    ImportIssueCode.SUB_CATEGORY_BLANK, "Sub Category is blank.", null));
        }
        if (row.getBrand().isEmpty()) {
            issues.add(issue(row.getSourceRowNumber(), "Brand", ImportIssueSeverity.INFO,
                    ImportIssueCode.BRAND_BLANK, "Brand is blank.", null));
        }
        if (row.getModel().isEmpty()) {
            issues.add(issue(row.getSourceRowNumber(), "Model", ImportIssueSeverity.INFO,
                    ImportIssueCode.MODEL_BLANK, "Model is blank.", null));
        }
        if (row.getPrimaryUser().isEmpty()) {
            issues.add(issue(row.getSourceRowNumber(), "Primary User", ImportIssueSeverity.INFO,
                    ImportIssueCode.PRIMARY_USER_BLANK, "Primary User is blank.", null));
        }
        if (row.getOwner().isEmpty()) {
            issues.add(issue(row.getSourceRowNumber(), "Owner", ImportIssueSeverity.WARNING,
                    ImportIssueCode.OWNER_BLANK, "Owner is blank.", null));
        }
        if (!row.getAuditDetails().isEmpty()) {
            issues.add(issue(row.getSourceRowNumber(), "Audit Details", ImportIssueSeverity.INFO,
                    ImportIssueCode.PRIOR_AUDIT_DETAILS_PRESENT, "Prior audit details are present and will be preserved.", null));
        }
        if (!row.getStatus().isEmpty() && !containsIgnoreCase(settings.getKnownStatuses(), row.getStatus())) {
            issues.add(issue(row.getSourceRowNumber(), "Status", ImportIssueSeverity.WARNING,
                    ImportIssueCode.UNKNOWN_STATUS, "Status is not in the configured known values.", row.getStatus()));
        }
        return fatal;
    }

    private boolean require(AssetImportRow row, String value, String column, ImportIssueCode code, List<ImportIssue> issues) {
        if (value == null || value.isEmpty()) {
            issues.add(issue(row.getSourceRowNumber(), column, ImportIssueSeverity.FATAL, code,
                    column + " is required.", null));
            return true;
        }
        return false;
    }

    private ImportValidationResult result(List<ImportIssue> issues, List<AssetImportRow> acceptedRows) {
        int fatal = 0;
        int warning = 0;
        int info = 0;
        for (ImportIssue issue : issues) {
            if (issue.getSeverity() == ImportIssueSeverity.FATAL) {
                fatal++;
            } else if (issue.getSeverity() == ImportIssueSeverity.WARNING) {
                warning++;
            } else {
                info++;
            }
        }
        return new ImportValidationResult(fatal == 0, fatal, warning, info, issues, acceptedRows);
    }

    private static ImportIssue issue(Integer rowNumber, String columnName, ImportIssueSeverity severity,
                                     ImportIssueCode code, String message, String sourceValue) {
        return new ImportIssue(rowNumber, columnName, severity, code, message, sourceValue);
    }

    private static boolean containsIgnoreCase(Set<String> values, String target) {
        String normalized = target.toLowerCase(Locale.ROOT);
        for (String value : values) {
            if (value.toLowerCase(Locale.ROOT).equals(normalized)) {
                return true;
            }
        }
        return false;
    }
}
