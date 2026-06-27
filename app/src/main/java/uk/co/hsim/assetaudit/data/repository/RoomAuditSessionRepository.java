package uk.co.hsim.assetaudit.data.repository;

import java.util.List;

import uk.co.hsim.assetaudit.data.dao.AuditSessionDao;
import uk.co.hsim.assetaudit.data.entity.AuditSessionEntity;
import uk.co.hsim.assetaudit.domain.enums.SessionStatus;
import uk.co.hsim.assetaudit.repository.AuditSessionRepository;

public final class RoomAuditSessionRepository implements AuditSessionRepository {
    private final AuditSessionDao dao;

    public RoomAuditSessionRepository(AuditSessionDao dao) {
        this.dao = dao;
    }

    @Override
    public void insert(AuditSessionEntity entity) {
        dao.insert(entity);
    }

    @Override
    public AuditSessionEntity getById(String sessionId) {
        return dao.getById(sessionId);
    }

    @Override
    public AuditSessionEntity getLatestActive() {
        return dao.getLatestActive();
    }

    @Override
    public List<AuditSessionEntity> listSessions() {
        return dao.listSessions();
    }

    @Override
    public void updateStatus(String sessionId, SessionStatus status, Long completedAtUtc) {
        dao.updateStatus(sessionId, status, completedAtUtc);
    }
}
