package uk.co.hsim.assetaudit.service;

public final class MovementConfirmationRequest {
    private final String sessionId;
    private final String assetTagId;
    private final String selectedDepartment;
    private final String expectedPreviousDepartment;
    private final String note;

    public MovementConfirmationRequest(String sessionId, String assetTagId, String selectedDepartment,
                                       String expectedPreviousDepartment, String note) {
        this.sessionId = sessionId;
        this.assetTagId = assetTagId;
        this.selectedDepartment = selectedDepartment;
        this.expectedPreviousDepartment = expectedPreviousDepartment;
        this.note = note;
    }

    public String getSessionId() {
        return sessionId;
    }

    public String getAssetTagId() {
        return assetTagId;
    }

    public String getSelectedDepartment() {
        return selectedDepartment;
    }

    public String getExpectedPreviousDepartment() {
        return expectedPreviousDepartment;
    }

    public String getNote() {
        return note;
    }
}
