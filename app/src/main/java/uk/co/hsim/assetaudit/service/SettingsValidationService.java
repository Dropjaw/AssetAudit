package uk.co.hsim.assetaudit.service;

import java.util.regex.Pattern;

import uk.co.hsim.assetaudit.domain.results.ErrorCode;
import uk.co.hsim.assetaudit.domain.results.OperationResult;
import uk.co.hsim.assetaudit.scanner.DataWedgeConstants;

public final class SettingsValidationService {
    private static final Pattern ACTION_PATTERN = Pattern.compile("[a-zA-Z][a-zA-Z0-9_.]*(\\.[A-Z][A-Z0-9_]*)+");
    private static final Pattern CONTROL_CHARS = Pattern.compile("[\\p{Cntrl}]");

    public OperationResult<String> validate(String packageName, String unassignedDepartmentLabel,
                                            String exportFormat, String profileName,
                                            String intentAction, String intentCategory) {
        if (!validLabel(unassignedDepartmentLabel, 80)) {
            return OperationResult.fail(ErrorCode.VALIDATION_FAILED, "Unassigned department label is invalid.");
        }
        if (!"CSV".equalsIgnoreCase(trim(exportFormat))) {
            return OperationResult.fail(ErrorCode.VALIDATION_FAILED, "Default export format must be CSV.");
        }
        if (!validLabel(profileName, 80)) {
            return OperationResult.fail(ErrorCode.VALIDATION_FAILED, "DataWedge profile name is invalid.");
        }
        String action = trim(intentAction);
        if (action.isEmpty() || action.length() > 120 || !action.startsWith(packageName + ".")
                || !ACTION_PATTERN.matcher(action).matches()) {
            return OperationResult.fail(ErrorCode.VALIDATION_FAILED,
                    "DataWedge action must start with " + packageName + ".");
        }
        String category = trim(intentCategory);
        if (!category.isEmpty() && !DataWedgeConstants.CATEGORY_SCAN.equals(category)) {
            return OperationResult.fail(ErrorCode.VALIDATION_FAILED,
                    "DataWedge category must be android.intent.category.DEFAULT.");
        }
        return OperationResult.ok("Settings are valid.");
    }

    private boolean validLabel(String value, int maxLength) {
        String trimmed = trim(value);
        return !trimmed.isEmpty()
                && trimmed.length() <= maxLength
                && !CONTROL_CHARS.matcher(trimmed).find();
    }

    private String trim(String value) {
        return value == null ? "" : value.trim();
    }
}
