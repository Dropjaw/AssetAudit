package uk.co.hsim.assetaudit.data.entity;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;

import uk.co.hsim.assetaudit.domain.enums.SessionStatus;

@Entity(
        tableName = "audit_session",
        indices = {
                @Index("status"),
                @Index("started_at_utc")
        }
)
public class AuditSessionEntity {
    @PrimaryKey
    @NonNull
    @ColumnInfo(name = "session_id")
    public String sessionId;

    @ColumnInfo(name = "audit_name")
    public String auditName;

    @ColumnInfo(name = "source_file_name")
    public String sourceFileName;

    @ColumnInfo(name = "source_file_uri")
    public String sourceFileUri;

    @ColumnInfo(name = "started_at_utc")
    public long startedAtUtc;

    @ColumnInfo(name = "completed_at_utc")
    public Long completedAtUtc;

    @NonNull
    @ColumnInfo(name = "status")
    public SessionStatus status = SessionStatus.DRAFT;

    @ColumnInfo(name = "created_by")
    public String createdBy;

    @ColumnInfo(name = "device_id")
    public String deviceId;

    @ColumnInfo(name = "schema_version")
    public int schemaVersion;

    public AuditSessionEntity(@NonNull String sessionId, String auditName, String sourceFileName,
                              String sourceFileUri, long startedAtUtc, Long completedAtUtc,
                              @NonNull SessionStatus status, String createdBy, String deviceId,
                              int schemaVersion) {
        this.sessionId = sessionId;
        this.auditName = auditName;
        this.sourceFileName = sourceFileName;
        this.sourceFileUri = sourceFileUri;
        this.startedAtUtc = startedAtUtc;
        this.completedAtUtc = completedAtUtc;
        this.status = status;
        this.createdBy = createdBy;
        this.deviceId = deviceId;
        this.schemaVersion = schemaVersion;
    }
}
