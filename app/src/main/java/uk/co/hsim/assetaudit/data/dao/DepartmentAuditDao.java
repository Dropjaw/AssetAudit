package uk.co.hsim.assetaudit.data.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import java.util.List;

import uk.co.hsim.assetaudit.data.entity.DepartmentAuditEntity;
import uk.co.hsim.assetaudit.domain.enums.DepartmentAuditStatus;

@Dao
public interface DepartmentAuditDao {
    @Insert(onConflict = OnConflictStrategy.ABORT)
    long insert(DepartmentAuditEntity entity);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    long upsert(DepartmentAuditEntity entity);

    @Query("SELECT * FROM department_audit WHERE session_id = :sessionId ORDER BY department_name")
    List<DepartmentAuditEntity> listBySession(String sessionId);

    @Query("SELECT * FROM department_audit WHERE session_id = :sessionId AND department_name = :departmentName LIMIT 1")
    DepartmentAuditEntity getDepartment(String sessionId, String departmentName);

    @Query("UPDATE department_audit SET status = :status, completed_at_utc = :completedAtUtc WHERE session_id = :sessionId AND department_name = :departmentName")
    void updateStatus(String sessionId, String departmentName, DepartmentAuditStatus status, Long completedAtUtc);

    @Query("UPDATE department_audit SET scanned_count = :scannedCount, status = :status, completed_at_utc = :completedAtUtc WHERE session_id = :sessionId AND department_name = :departmentName")
    int updateProgress(String sessionId, String departmentName, int scannedCount, DepartmentAuditStatus status, Long completedAtUtc);

    @Update
    void update(DepartmentAuditEntity entity);
}
