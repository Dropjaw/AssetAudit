package uk.co.hsim.assetaudit.repository;

import java.util.List;

import uk.co.hsim.assetaudit.data.entity.AssetEntity;
import uk.co.hsim.assetaudit.domain.enums.AuditStatus;

public interface AssetRepository {
    long insert(AssetEntity entity);

    List<Long> insertAll(List<AssetEntity> entities);

    AssetEntity getByTag(String sessionId, String assetTagId);

    int countBySession(String sessionId);

    int countByDepartmentAndStatus(String sessionId, String department, AuditStatus auditStatus);
}
