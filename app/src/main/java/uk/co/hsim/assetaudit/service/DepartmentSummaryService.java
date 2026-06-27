package uk.co.hsim.assetaudit.service;

import java.util.List;

import uk.co.hsim.assetaudit.data.entity.DepartmentAuditEntity;
import uk.co.hsim.assetaudit.repository.DepartmentAuditRepository;

public final class DepartmentSummaryService {
    private final DepartmentAuditRepository repository;

    public DepartmentSummaryService(DepartmentAuditRepository repository) {
        this.repository = repository;
    }

    public List<DepartmentAuditEntity> getDepartmentSummaries(String sessionId) {
        return repository.listBySession(sessionId);
    }
}
