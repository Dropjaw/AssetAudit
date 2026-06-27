package uk.co.hsim.assetaudit.repository;

import java.util.List;

import uk.co.hsim.assetaudit.data.entity.DepartmentAuditEntity;
import uk.co.hsim.assetaudit.domain.enums.DepartmentAuditStatus;

public interface DepartmentAuditRepository {
    long insert(DepartmentAuditEntity entity);

    List<DepartmentAuditEntity> listBySession(String sessionId);

    DepartmentAuditEntity getDepartment(String sessionId, String departmentName);

    void updateStatus(String sessionId, String departmentName, DepartmentAuditStatus status, Long completedAtUtc);

    int updateProgress(String sessionId, String departmentName, int scannedCount, DepartmentAuditStatus status, Long completedAtUtc);
}
