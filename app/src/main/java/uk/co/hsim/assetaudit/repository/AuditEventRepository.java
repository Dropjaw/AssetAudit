package uk.co.hsim.assetaudit.repository;

import java.util.List;

import uk.co.hsim.assetaudit.data.entity.AuditEventEntity;

public interface AuditEventRepository {
    void append(AuditEventEntity entity);

    List<AuditEventEntity> listBySession(String sessionId);

    List<AuditEventEntity> listByAssetTag(String sessionId, String assetTagId);
}
