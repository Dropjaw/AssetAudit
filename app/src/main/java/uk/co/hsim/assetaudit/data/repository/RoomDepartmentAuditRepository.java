package uk.co.hsim.assetaudit.data.repository;

import java.util.List;

import uk.co.hsim.assetaudit.data.dao.DepartmentAuditDao;
import uk.co.hsim.assetaudit.data.entity.DepartmentAuditEntity;
import uk.co.hsim.assetaudit.domain.enums.DepartmentAuditStatus;
import uk.co.hsim.assetaudit.repository.DepartmentAuditRepository;

public final class RoomDepartmentAuditRepository implements DepartmentAuditRepository {
    private final DepartmentAuditDao dao;

    public RoomDepartmentAuditRepository(DepartmentAuditDao dao) {
        this.dao = dao;
    }

    @Override
    public long insert(DepartmentAuditEntity entity) {
        return dao.insert(entity);
    }

    @Override
    public List<DepartmentAuditEntity> listBySession(String sessionId) {
        return dao.listBySession(sessionId);
    }

    @Override
    public DepartmentAuditEntity getDepartment(String sessionId, String departmentName) {
        return dao.getDepartment(sessionId, departmentName);
    }

    @Override
    public void updateStatus(String sessionId, String departmentName, DepartmentAuditStatus status, Long completedAtUtc) {
        dao.updateStatus(sessionId, departmentName, status, completedAtUtc);
    }
}
