package uk.co.hsim.assetaudit.service;

import uk.co.hsim.assetaudit.domain.enums.SessionStatus;

public final class AuditCompletionResult {
    private final String sessionId;
    private final SessionStatus finalStatus;
    private final boolean changed;
    private final String message;
    private final AuditCompletionState state;

    public AuditCompletionResult(String sessionId, SessionStatus finalStatus, boolean changed,
                                 String message, AuditCompletionState state) {
        this.sessionId = sessionId;
        this.finalStatus = finalStatus;
        this.changed = changed;
        this.message = message == null ? "" : message;
        this.state = state;
    }

    public String getSessionId() { return sessionId; }
    public SessionStatus getFinalStatus() { return finalStatus; }
    public boolean isChanged() { return changed; }
    public String getMessage() { return message; }
    public AuditCompletionState getState() { return state; }
}
