package uk.co.hsim.assetaudit.data.entity;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.PrimaryKey;

import uk.co.hsim.assetaudit.domain.enums.DepartmentAuditStatus;

@Entity(
        tableName = "department_audit",
        foreignKeys = @ForeignKey(
                entity = AuditSessionEntity.class,
                parentColumns = "session_id",
                childColumns = "session_id",
                onDelete = ForeignKey.CASCADE
        ),
        indices = @Index(value = {"session_id", "department_name"}, unique = true)
)
public class DepartmentAuditEntity {
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "department_audit_id")
    public long departmentAuditId;

    @NonNull
    @ColumnInfo(name = "session_id")
    public String sessionId;

    @NonNull
    @ColumnInfo(name = "department_name")
    public String departmentName;

    @ColumnInfo(name = "expected_count")
    public int expectedCount;

    @ColumnInfo(name = "scanned_count")
    public int scannedCount;

    @ColumnInfo(name = "missing_count")
    public int missingCount;

    @ColumnInfo(name = "moved_in_count")
    public int movedInCount;

    @ColumnInfo(name = "new_asset_count")
    public int newAssetCount;

    @NonNull
    @ColumnInfo(name = "status")
    public DepartmentAuditStatus status = DepartmentAuditStatus.NOT_STARTED;

    @ColumnInfo(name = "completed_at_utc")
    public Long completedAtUtc;

    public DepartmentAuditEntity(@NonNull String sessionId, @NonNull String departmentName,
                                 int expectedCount, int scannedCount, int missingCount,
                                 int movedInCount, int newAssetCount,
                                 @NonNull DepartmentAuditStatus status, Long completedAtUtc) {
        this.sessionId = sessionId;
        this.departmentName = departmentName;
        this.expectedCount = expectedCount;
        this.scannedCount = scannedCount;
        this.missingCount = missingCount;
        this.movedInCount = movedInCount;
        this.newAssetCount = newAssetCount;
        this.status = status;
        this.completedAtUtc = completedAtUtc;
    }
}
