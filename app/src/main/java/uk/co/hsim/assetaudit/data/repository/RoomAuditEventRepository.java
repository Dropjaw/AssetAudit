package uk.co.hsim.assetaudit.data.repository;

import java.util.List;

import uk.co.hsim.assetaudit.data.dao.AuditEventDao;
import uk.co.hsim.assetaudit.data.entity.AuditEventEntity;
import uk.co.hsim.assetaudit.repository.AuditEventRepository;

public final class RoomAuditEventRepository implements AuditEventRepository {
    private final AuditEventDao dao;

    public RoomAuditEventRepository(AuditEventDao dao) {
        this.dao = dao;
    }

    @Override
    public void append(AuditEventEntity entity) {
        dao.insert(entity);
    }

    @Override
    public List<AuditEventEntity> listBySession(String sessionId) {
        return dao.listBySession(sessionId);
    }

    @Override
    public List<AuditEventEntity> listByAssetTag(String sessionId, String assetTagId) {
        return dao.listByAssetTag(sessionId, assetTagId);
    }
}
