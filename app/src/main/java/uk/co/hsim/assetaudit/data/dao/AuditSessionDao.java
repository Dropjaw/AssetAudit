package uk.co.hsim.assetaudit.data.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import java.util.List;

import uk.co.hsim.assetaudit.data.entity.AuditSessionEntity;
import uk.co.hsim.assetaudit.domain.enums.SessionStatus;

@Dao
public interface AuditSessionDao {
    @Insert(onConflict = OnConflictStrategy.ABORT)
    void insert(AuditSessionEntity entity);

    @Query("SELECT * FROM audit_session WHERE session_id = :sessionId LIMIT 1")
    AuditSessionEntity getById(String sessionId);

    @Query("SELECT * FROM audit_session WHERE status = 'ACTIVE' ORDER BY started_at_utc DESC LIMIT 1")
    AuditSessionEntity getLatestActive();

    @Query("SELECT * FROM audit_session ORDER BY started_at_utc DESC")
    List<AuditSessionEntity> listSessions();

    @Query("UPDATE audit_session SET status = :status, completed_at_utc = :completedAtUtc WHERE session_id = :sessionId")
    void updateStatus(String sessionId, SessionStatus status, Long completedAtUtc);
}
