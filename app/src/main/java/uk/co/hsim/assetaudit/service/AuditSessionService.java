package uk.co.hsim.assetaudit.service;

import uk.co.hsim.assetaudit.data.entity.AuditSessionEntity;
import uk.co.hsim.assetaudit.repository.AuditSessionRepository;

public final class AuditSessionService {
    private final AuditSessionRepository repository;

    public AuditSessionService(AuditSessionRepository repository) {
        this.repository = repository;
    }

    public AuditSessionEntity getActiveSession() {
        return repository.getLatestActive();
    }
}
