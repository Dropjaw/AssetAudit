package uk.co.hsim.assetaudit.data.entity;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.PrimaryKey;

@Entity(
        tableName = "export_run",
        foreignKeys = @ForeignKey(
                entity = AuditSessionEntity.class,
                parentColumns = "session_id",
                childColumns = "session_id",
                onDelete = ForeignKey.CASCADE
        ),
        indices = @Index(value = {"session_id", "exported_at_utc"})
)
public class ExportRunEntity {
    @PrimaryKey
    @NonNull
    @ColumnInfo(name = "export_run_id")
    public String exportRunId;

    @NonNull
    @ColumnInfo(name = "session_id")
    public String sessionId;

    @ColumnInfo(name = "package_id")
    public String packageId;

    @ColumnInfo(name = "destination_display_name")
    public String destinationDisplayName;

    @ColumnInfo(name = "export_mode")
    public String exportMode;

    @ColumnInfo(name = "readiness")
    public String readiness;

    @ColumnInfo(name = "exported_at_utc")
    public long exportedAtUtc;

    @ColumnInfo(name = "app_version_name")
    public String appVersionName;

    @ColumnInfo(name = "device_id")
    public String deviceId;

    @ColumnInfo(name = "user_name")
    public String userName;

    @ColumnInfo(name = "manifest_json")
    public String manifestJson;

    public ExportRunEntity(@NonNull String exportRunId, @NonNull String sessionId, String packageId,
                           String destinationDisplayName, String exportMode, String readiness,
                           long exportedAtUtc, String appVersionName, String deviceId,
                           String userName, String manifestJson) {
        this.exportRunId = exportRunId;
        this.sessionId = sessionId;
        this.packageId = packageId;
        this.destinationDisplayName = destinationDisplayName;
        this.exportMode = exportMode;
        this.readiness = readiness;
        this.exportedAtUtc = exportedAtUtc;
        this.appVersionName = appVersionName;
        this.deviceId = deviceId;
        this.userName = userName;
        this.manifestJson = manifestJson;
    }
}
