package uk.co.hsim.assetaudit.service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class SkipAssetsRequest {
    private final String sessionId;
    private final String department;
    private final List<String> assetTagIds;
    private final String reason;

    public SkipAssetsRequest(String sessionId, String department, List<String> assetTagIds, String reason) {
        this.sessionId = sessionId;
        this.department = department;
        this.assetTagIds = Collections.unmodifiableList(new ArrayList<>(assetTagIds));
        this.reason = reason == null ? "" : reason.trim();
    }

    public String getSessionId() {
        return sessionId;
    }

    public String getDepartment() {
        return department;
    }

    public List<String> getAssetTagIds() {
        return assetTagIds;
    }

    public String getReason() {
        return reason;
    }
}
