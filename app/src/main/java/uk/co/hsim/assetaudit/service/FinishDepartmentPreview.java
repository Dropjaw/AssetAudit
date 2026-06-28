package uk.co.hsim.assetaudit.service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import uk.co.hsim.assetaudit.data.entity.AssetEntity;

public final class FinishDepartmentPreview {
    private final String sessionId;
    private final String department;
    private final int expectedCount;
    private final int scannedCount;
    private final int remainingCount;
    private final List<AssetEntity> remainingAssets;
    private final ExceptionCountSummary exceptionCounts;

    public FinishDepartmentPreview(String sessionId, String department, int expectedCount, int scannedCount,
                                   List<AssetEntity> remainingAssets, ExceptionCountSummary exceptionCounts) {
        this.sessionId = sessionId;
        this.department = department;
        this.expectedCount = expectedCount;
        this.scannedCount = scannedCount;
        this.remainingAssets = Collections.unmodifiableList(new ArrayList<>(remainingAssets));
        this.remainingCount = remainingAssets.size();
        this.exceptionCounts = exceptionCounts;
    }

    public String getSessionId() {
        return sessionId;
    }

    public String getDepartment() {
        return department;
    }

    public int getExpectedCount() {
        return expectedCount;
    }

    public int getScannedCount() {
        return scannedCount;
    }

    public int getRemainingCount() {
        return remainingCount;
    }

    public List<AssetEntity> getRemainingAssets() {
        return remainingAssets;
    }

    public ExceptionCountSummary getExceptionCounts() {
        return exceptionCounts;
    }
}
