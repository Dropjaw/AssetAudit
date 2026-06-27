package uk.co.hsim.assetaudit.service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import uk.co.hsim.assetaudit.data.entity.AssetEntity;

public final class DepartmentAuditContext {
    private final String sessionId;
    private final String sessionName;
    private final String departmentName;
    private final DepartmentDashboardRow progress;
    private final List<AssetEntity> remainingAssets;

    public DepartmentAuditContext(String sessionId, String sessionName, String departmentName,
                                  DepartmentDashboardRow progress, List<AssetEntity> remainingAssets) {
        this.sessionId = sessionId;
        this.sessionName = sessionName;
        this.departmentName = departmentName;
        this.progress = progress;
        this.remainingAssets = Collections.unmodifiableList(new ArrayList<>(remainingAssets));
    }

    public String getSessionId() {
        return sessionId;
    }

    public String getSessionName() {
        return sessionName;
    }

    public String getDepartmentName() {
        return departmentName;
    }

    public DepartmentDashboardRow getProgress() {
        return progress;
    }

    public List<AssetEntity> getRemainingAssets() {
        return remainingAssets;
    }
}
