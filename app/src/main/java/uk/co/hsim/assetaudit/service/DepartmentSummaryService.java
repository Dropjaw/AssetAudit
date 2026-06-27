package uk.co.hsim.assetaudit.service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import uk.co.hsim.assetaudit.data.entity.AssetEntity;
import uk.co.hsim.assetaudit.data.entity.DepartmentAuditEntity;
import uk.co.hsim.assetaudit.domain.enums.AuditStatus;
import uk.co.hsim.assetaudit.domain.enums.DepartmentAuditStatus;
import uk.co.hsim.assetaudit.repository.AssetRepository;
import uk.co.hsim.assetaudit.repository.DepartmentAuditRepository;

public final class DepartmentSummaryService {
    private final DepartmentAuditRepository repository;
    private final AssetRepository assetRepository;

    public DepartmentSummaryService(DepartmentAuditRepository repository, AssetRepository assetRepository) {
        this.repository = repository;
        this.assetRepository = assetRepository;
    }

    public List<DepartmentAuditEntity> getDepartmentSummaries(String sessionId) {
        return repository.listBySession(sessionId);
    }

    public List<DepartmentDashboardRow> getDashboardRows(String sessionId) {
        List<DepartmentDashboardRow> rows = new ArrayList<>();
        for (DepartmentAuditEntity entity : repository.listBySession(sessionId)) {
            int remaining = assetRepository.countByDepartmentAndStatus(
                    sessionId,
                    entity.departmentName,
                    AuditStatus.NOT_AUDITED
            );
            int issueCount = entity.missingCount + entity.movedInCount + entity.newAssetCount;
            rows.add(new DepartmentDashboardRow(
                    entity.departmentName,
                    entity.expectedCount,
                    entity.scannedCount,
                    remaining,
                    issueCount,
                    entity.status
            ));
        }
        Collections.sort(rows, dashboardComparator());
        return rows;
    }

    public DepartmentAuditContext getDepartmentAuditContext(String sessionId, String sessionName, String departmentName) {
        DepartmentAuditEntity department = repository.getDepartment(sessionId, departmentName);
        List<AssetEntity> remainingAssets = assetRepository.listByDepartmentAndStatus(
                sessionId,
                departmentName,
                AuditStatus.NOT_AUDITED
        );
        DepartmentDashboardRow row;
        if (department == null) {
            row = new DepartmentDashboardRow(departmentName, 0, 0, remainingAssets.size(), 0, DepartmentAuditStatus.NOT_STARTED);
        } else {
            row = new DepartmentDashboardRow(
                    department.departmentName,
                    department.expectedCount,
                    department.scannedCount,
                    remainingAssets.size(),
                    department.missingCount + department.movedInCount + department.newAssetCount,
                    department.status
            );
        }
        return new DepartmentAuditContext(sessionId, sessionName, departmentName, row, remainingAssets);
    }

    private Comparator<DepartmentDashboardRow> dashboardComparator() {
        return (left, right) -> {
            boolean leftComplete = left.getRemainingCount() == 0;
            boolean rightComplete = right.getRemainingCount() == 0;
            if (leftComplete != rightComplete) {
                return leftComplete ? 1 : -1;
            }
            int remainingCompare = Integer.compare(right.getRemainingCount(), left.getRemainingCount());
            if (remainingCompare != 0) {
                return remainingCompare;
            }
            return left.getDepartmentName().compareToIgnoreCase(right.getDepartmentName());
        };
    }
}
