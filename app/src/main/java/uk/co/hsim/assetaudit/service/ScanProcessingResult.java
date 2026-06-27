package uk.co.hsim.assetaudit.service;

import uk.co.hsim.assetaudit.domain.enums.ScanResultType;

public final class ScanProcessingResult {
    private final boolean success;
    private final ScanResultType resultType;
    private final String assetTagId;
    private final String selectedDepartment;
    private final String assetDepartment;
    private final String description;
    private final String message;
    private final long timestampUtc;

    private ScanProcessingResult(boolean success, ScanResultType resultType, String assetTagId,
                                 String selectedDepartment, String assetDepartment, String description,
                                 String message, long timestampUtc) {
        this.success = success;
        this.resultType = resultType;
        this.assetTagId = assetTagId;
        this.selectedDepartment = selectedDepartment;
        this.assetDepartment = assetDepartment;
        this.description = description;
        this.message = message;
        this.timestampUtc = timestampUtc;
    }

    public static ScanProcessingResult successExpected(String assetTagId, String selectedDepartment,
                                                       String description, long timestampUtc) {
        return new ScanProcessingResult(true, ScanResultType.SUCCESS_EXPECTED, assetTagId,
                selectedDepartment, selectedDepartment, description,
                "Accepted: asset audited in this department.", timestampUtc);
    }

    public static ScanProcessingResult duplicate(String assetTagId, String selectedDepartment,
                                                 String assetDepartment, String description, long timestampUtc) {
        return new ScanProcessingResult(false, ScanResultType.DUPLICATE_SCAN, assetTagId,
                selectedDepartment, assetDepartment, description,
                "Already audited: no changes made.", timestampUtc);
    }

    public static ScanProcessingResult foundOtherDepartment(String assetTagId, String selectedDepartment,
                                                            String assetDepartment, String description, long timestampUtc) {
        return new ScanProcessingResult(false, ScanResultType.FOUND_IN_OTHER_DEPARTMENT_REQUIRES_CONFIRMATION,
                assetTagId, selectedDepartment, assetDepartment, description,
                "Asset belongs to " + assetDepartment + ". Movement confirmation arrives in Phase 5.", timestampUtc);
    }

    public static ScanProcessingResult unknown(String assetTagId, String selectedDepartment, long timestampUtc) {
        return new ScanProcessingResult(false, ScanResultType.UNKNOWN_ASSET_REQUIRES_INPUT, assetTagId,
                selectedDepartment, null, null,
                "Unknown asset. New asset entry arrives in Phase 5.", timestampUtc);
    }

    public static ScanProcessingResult invalid(String selectedDepartment, long timestampUtc) {
        return new ScanProcessingResult(false, ScanResultType.INVALID_SCAN, "",
                selectedDepartment, null, null,
                "Invalid scan: enter a non-empty asset tag.", timestampUtc);
    }

    public static ScanProcessingResult error(String assetTagId, String selectedDepartment, String message, long timestampUtc) {
        return new ScanProcessingResult(false, ScanResultType.ERROR, assetTagId,
                selectedDepartment, null, null, message, timestampUtc);
    }

    public boolean isSuccess() {
        return success;
    }

    public ScanResultType getResultType() {
        return resultType;
    }

    public String getAssetTagId() {
        return assetTagId;
    }

    public String getSelectedDepartment() {
        return selectedDepartment;
    }

    public String getAssetDepartment() {
        return assetDepartment;
    }

    public String getDescription() {
        return description;
    }

    public String getMessage() {
        return message;
    }

    public long getTimestampUtc() {
        return timestampUtc;
    }
}
