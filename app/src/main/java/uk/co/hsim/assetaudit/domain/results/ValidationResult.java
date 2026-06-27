package uk.co.hsim.assetaudit.domain.results;

import java.util.Collections;
import java.util.List;

import uk.co.hsim.assetaudit.domain.enums.ImportIssueSeverity;

public final class ValidationResult {
    private final List<ValidationIssue> issues;

    public ValidationResult(List<ValidationIssue> issues) {
        this.issues = issues == null ? Collections.emptyList() : Collections.unmodifiableList(issues);
    }

    public boolean isValid() {
        for (ValidationIssue issue : issues) {
            if (issue.getSeverity() == ImportIssueSeverity.FATAL) {
                return false;
            }
        }
        return true;
    }

    public List<ValidationIssue> getIssues() {
        return issues;
    }
}
