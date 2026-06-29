package uk.co.hsim.assetaudit.service;

public final class ForceCloseAuditRequest {
    private final String sessionId;
    private final String reason;
    private final boolean userConfirmed;

    public ForceCloseAuditRequest(String sessionId, String reason, boolean userConfirmed) {
        this.sessionId = sessionId;
        this.reason = reason == null ? "" : reason;
        this.userConfirmed = userConfirmed;
    }

    public String getSessionId() {
        return sessionId;
    }

    public String getReason() {
        return reason;
    }

    public boolean isUserConfirmed() {
        return userConfirmed;
    }
}
