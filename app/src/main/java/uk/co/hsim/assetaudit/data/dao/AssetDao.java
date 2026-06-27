package uk.co.hsim.assetaudit.data.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import java.util.List;

import uk.co.hsim.assetaudit.data.entity.AssetEntity;
import uk.co.hsim.assetaudit.domain.enums.AuditStatus;

@Dao
public interface AssetDao {
    @Insert(onConflict = OnConflictStrategy.ABORT)
    long insert(AssetEntity entity);

    @Insert(onConflict = OnConflictStrategy.ABORT)
    List<Long> insertAll(List<AssetEntity> entities);

    @Query("SELECT * FROM asset WHERE session_id = :sessionId AND asset_tag_id = :assetTagId LIMIT 1")
    AssetEntity getByTag(String sessionId, String assetTagId);

    @Query("SELECT COUNT(*) FROM asset WHERE session_id = :sessionId")
    int countBySession(String sessionId);

    @Query("SELECT COUNT(*) FROM asset WHERE session_id = :sessionId AND department = :department AND audit_status = :auditStatus")
    int countByDepartmentAndStatus(String sessionId, String department, AuditStatus auditStatus);

    @Query("SELECT * FROM asset WHERE session_id = :sessionId AND department = :department AND audit_status = :auditStatus ORDER BY asset_tag_id")
    List<AssetEntity> listByDepartmentAndStatus(String sessionId, String department, AuditStatus auditStatus);

    @Query("UPDATE asset SET audit_status = :auditStatus, updated_at_utc = :updatedAtUtc WHERE session_id = :sessionId AND asset_tag_id = :assetTagId")
    int updateAuditStatus(String sessionId, String assetTagId, AuditStatus auditStatus, long updatedAtUtc);
}
