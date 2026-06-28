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

    @Query("SELECT * FROM asset WHERE session_id = :sessionId ORDER BY CASE WHEN source_row_number IS NULL THEN 1 ELSE 0 END, source_row_number ASC, created_during_audit ASC, asset_tag_id ASC")
    List<AssetEntity> listBySessionForExport(String sessionId);

    @Query("SELECT COUNT(*) FROM asset WHERE session_id = :sessionId AND audit_status = :auditStatus")
    int countByStatus(String sessionId, AuditStatus auditStatus);

    @Query("SELECT COUNT(*) FROM asset WHERE session_id = :sessionId AND created_during_audit = 1")
    int countCreatedDuringAudit(String sessionId);

    @Query("SELECT COUNT(*) FROM asset WHERE session_id = :sessionId AND department = :department AND audit_status = :auditStatus")
    int countByDepartmentAndStatus(String sessionId, String department, AuditStatus auditStatus);

    @Query("SELECT * FROM asset WHERE session_id = :sessionId AND department = :department AND audit_status = :auditStatus ORDER BY asset_tag_id")
    List<AssetEntity> listByDepartmentAndStatus(String sessionId, String department, AuditStatus auditStatus);

    @Query("UPDATE asset SET audit_status = :auditStatus, updated_at_utc = :updatedAtUtc WHERE session_id = :sessionId AND asset_tag_id = :assetTagId")
    int updateAuditStatus(String sessionId, String assetTagId, AuditStatus auditStatus, long updatedAtUtc);

    @Query("UPDATE asset SET department = :newDepartment, previous_department = :previousDepartment, audit_status = :auditStatus, updated_at_utc = :updatedAtUtc WHERE session_id = :sessionId AND asset_tag_id = :assetTagId AND department = :expectedCurrentDepartment AND audit_status = :expectedStatus")
    int confirmMovedDepartment(String sessionId, String assetTagId, String expectedCurrentDepartment,
                               String newDepartment, String previousDepartment, AuditStatus expectedStatus,
                               AuditStatus auditStatus, long updatedAtUtc);

    @Query("UPDATE asset SET audit_status = :newStatus, updated_at_utc = :updatedAtUtc WHERE session_id = :sessionId AND asset_tag_id = :assetTagId AND department = :department AND audit_status = :expectedStatus")
    int updateStatusIfCurrent(String sessionId, String assetTagId, String department,
                              AuditStatus expectedStatus, AuditStatus newStatus, long updatedAtUtc);
}
