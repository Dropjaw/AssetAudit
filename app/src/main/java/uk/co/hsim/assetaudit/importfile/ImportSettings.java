package uk.co.hsim.assetaudit.importfile;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public final class ImportSettings {
    private final String unassignedDepartmentLabel;
    private final Set<String> knownStatuses;

    public ImportSettings(String unassignedDepartmentLabel, Set<String> knownStatuses) {
        this.unassignedDepartmentLabel = unassignedDepartmentLabel == null || unassignedDepartmentLabel.trim().isEmpty()
                ? "Unassigned / Blank Department"
                : unassignedDepartmentLabel.trim();
        this.knownStatuses = new HashSet<>(knownStatuses);
    }

    public static ImportSettings defaults(String unassignedDepartmentLabel) {
        return new ImportSettings(unassignedDepartmentLabel, new HashSet<>(Arrays.asList("Available", "Checked out")));
    }

    public String getUnassignedDepartmentLabel() {
        return unassignedDepartmentLabel;
    }

    public Set<String> getKnownStatuses() {
        return knownStatuses;
    }
}
