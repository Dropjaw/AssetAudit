package uk.co.hsim.assetaudit.service;

import uk.co.hsim.assetaudit.domain.enums.DepartmentAuditStatus;

public final class DepartmentDashboardRow {
    private final String departmentName;
    private final int expectedCount;
    private final int scannedCount;
    private final int remainingCount;
    private final int issueCount;
    private final DepartmentAuditStatus status;

    public DepartmentDashboardRow(String departmentName, int expectedCount, int scannedCount,
                                  int remainingCount, int issueCount, DepartmentAuditStatus status) {
        this.departmentName = departmentName;
        this.expectedCount = expectedCount;
        this.scannedCount = scannedCount;
        this.remainingCount = remainingCount;
        this.issueCount = issueCount;
        this.status = status;
    }

    public String getDepartmentName() {
        return departmentName;
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

    public int getIssueCount() {
        return issueCount;
    }

    public DepartmentAuditStatus getStatus() {
        return status;
    }
}
