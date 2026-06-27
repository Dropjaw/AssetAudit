package uk.co.hsim.assetaudit.domain.rules;

public final class DepartmentNameNormalizer {
    private DepartmentNameNormalizer() {
    }

    public static String normalizeDepartmentName(String raw) {
        if (raw == null) {
            return "";
        }
        return raw.trim();
    }

    public static boolean isBlankDepartment(String raw) {
        return normalizeDepartmentName(raw).isEmpty();
    }
}
