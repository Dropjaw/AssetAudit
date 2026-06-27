package uk.co.hsim.assetaudit.data.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import java.util.List;

import uk.co.hsim.assetaudit.data.entity.AuditEventEntity;

@Dao
public interface AuditEventDao {
    @Insert(onConflict = OnConflictStrategy.ABORT)
    void insert(AuditEventEntity entity);

    @Query("SELECT * FROM audit_event WHERE session_id = :sessionId ORDER BY timestamp_utc ASC, event_id ASC")
    List<AuditEventEntity> listBySession(String sessionId);

    @Query("SELECT * FROM audit_event WHERE session_id = :sessionId AND asset_tag_id = :assetTagId ORDER BY timestamp_utc ASC, event_id ASC")
    List<AuditEventEntity> listByAssetTag(String sessionId, String assetTagId);

    @Query("SELECT COUNT(*) FROM audit_event WHERE session_id = :sessionId")
    int countBySession(String sessionId);
}
