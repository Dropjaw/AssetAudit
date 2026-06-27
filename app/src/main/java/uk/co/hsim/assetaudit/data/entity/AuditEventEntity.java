package uk.co.hsim.assetaudit.data.entity;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.PrimaryKey;

import uk.co.hsim.assetaudit.domain.enums.EventKind;
import uk.co.hsim.assetaudit.domain.enums.ScanResultType;

@Entity(
        tableName = "audit_event",
        foreignKeys = @ForeignKey(
                entity = AuditSessionEntity.class,
                parentColumns = "session_id",
                childColumns = "session_id",
                onDelete = ForeignKey.CASCADE
        ),
        indices = {
                @Index(value = {"session_id", "asset_tag_id"}),
                @Index(value = {"session_id", "timestamp_utc"})
        }
)
public class AuditEventEntity {
    @PrimaryKey
    @NonNull
    @ColumnInfo(name = "event_id")
    public String eventId;

    @NonNull
    @ColumnInfo(name = "session_id")
    public String sessionId;

    @ColumnInfo(name = "asset_tag_id")
    public String assetTagId;

    @NonNull
    @ColumnInfo(name = "event_kind")
    public EventKind eventKind;

    @ColumnInfo(name = "result_type")
    public ScanResultType resultType;

    @ColumnInfo(name = "selected_department")
    public String selectedDepartment;

    @ColumnInfo(name = "previous_department")
    public String previousDepartment;

    @ColumnInfo(name = "timestamp_utc")
    public long timestampUtc;

    @ColumnInfo(name = "user_name")
    public String userName;

    @ColumnInfo(name = "device_id")
    public String deviceId;

    @ColumnInfo(name = "notes")
    public String notes;

    public AuditEventEntity(@NonNull String eventId, @NonNull String sessionId, String assetTagId,
                            @NonNull EventKind eventKind, ScanResultType resultType,
                            String selectedDepartment, String previousDepartment, long timestampUtc,
                            String userName, String deviceId, String notes) {
        this.eventId = eventId;
        this.sessionId = sessionId;
        this.assetTagId = assetTagId;
        this.eventKind = eventKind;
        this.resultType = resultType;
        this.selectedDepartment = selectedDepartment;
        this.previousDepartment = previousDepartment;
        this.timestampUtc = timestampUtc;
        this.userName = userName;
        this.deviceId = deviceId;
        this.notes = notes;
    }
}
