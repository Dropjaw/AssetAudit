package uk.co.hsim.assetaudit.repository;

import java.util.List;

import uk.co.hsim.assetaudit.data.entity.AuditSessionEntity;
import uk.co.hsim.assetaudit.domain.enums.SessionStatus;

public interface AuditSessionRepository {
    void insert(AuditSessionEntity entity);

    AuditSessionEntity getById(String sessionId);

    AuditSessionEntity getLatestActive();

    List<AuditSessionEntity> listSessions();

    void updateStatus(String sessionId, SessionStatus status, Long completedAtUtc);
}
