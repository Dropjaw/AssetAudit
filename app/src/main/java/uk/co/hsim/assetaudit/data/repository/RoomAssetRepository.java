package uk.co.hsim.assetaudit.data.repository;

import java.util.List;

import uk.co.hsim.assetaudit.data.dao.AssetDao;
import uk.co.hsim.assetaudit.data.entity.AssetEntity;
import uk.co.hsim.assetaudit.domain.enums.AuditStatus;
import uk.co.hsim.assetaudit.repository.AssetRepository;

public final class RoomAssetRepository implements AssetRepository {
    private final AssetDao dao;

    public RoomAssetRepository(AssetDao dao) {
        this.dao = dao;
    }

    @Override
    public long insert(AssetEntity entity) {
        return dao.insert(entity);
    }

    @Override
    public List<Long> insertAll(List<AssetEntity> entities) {
        return dao.insertAll(entities);
    }

    @Override
    public AssetEntity getByTag(String sessionId, String assetTagId) {
        return dao.getByTag(sessionId, assetTagId);
    }

    @Override
    public int countBySession(String sessionId) {
        return dao.countBySession(sessionId);
    }

    @Override
    public int countByDepartmentAndStatus(String sessionId, String department, AuditStatus auditStatus) {
        return dao.countByDepartmentAndStatus(sessionId, department, auditStatus);
    }
}
