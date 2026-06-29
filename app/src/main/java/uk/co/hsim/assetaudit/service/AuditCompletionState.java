package uk.co.hsim.assetaudit.service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import uk.co.hsim.assetaudit.domain.enums.SessionStatus;

public final class AuditCompletionState {
    private final String sessionId;
    private final SessionStatus sessionStatus;
    private final int totalDepartments;
    private final int completeDepartments;
    private final int incompleteDepartments;
    private final int remainingAssets;
    private final boolean hasFinalExport;
    private final boolean hasSessionExportedEvent;
    private final boolean hasSessionCompletedEvent;
    private final boolean eligibleForNormalCompletion;
    private final List<String> blockers;

    public AuditCompletionState(String sessionId, SessionStatus sessionStatus, int totalDepartments,
                                int completeDepartments, int incompleteDepartments, int remainingAssets,
                                boolean hasFinalExport, boolean hasSessionExportedEvent,
                                boolean hasSessionCompletedEvent, boolean eligibleForNormalCompletion,
                                List<String> blockers) {
        this.sessionId = sessionId;
        this.sessionStatus = sessionStatus;
        this.totalDepartments = totalDepartments;
        this.completeDepartments = completeDepartments;
        this.incompleteDepartments = incompleteDepartments;
        this.remainingAssets = remainingAssets;
        this.hasFinalExport = hasFinalExport;
        this.hasSessionExportedEvent = hasSessionExportedEvent;
        this.hasSessionCompletedEvent = hasSessionCompletedEvent;
        this.eligibleForNormalCompletion = eligibleForNormalCompletion;
        this.blockers = Collections.unmodifiableList(new ArrayList<>(blockers));
    }

    public String getSessionId() { return sessionId; }
    public SessionStatus getSessionStatus() { return sessionStatus; }
    public int getTotalDepartments() { return totalDepartments; }
    public int getCompleteDepartments() { return completeDepartments; }
    public int getIncompleteDepartments() { return incompleteDepartments; }
    public int getRemainingAssets() { return remainingAssets; }
    public boolean hasFinalExport() { return hasFinalExport; }
    public boolean hasSessionExportedEvent() { return hasSessionExportedEvent; }
    public boolean hasSessionCompletedEvent() { return hasSessionCompletedEvent; }
    public boolean isEligibleForNormalCompletion() { return eligibleForNormalCompletion; }
    public List<String> getBlockers() { return blockers; }
}
