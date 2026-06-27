package uk.co.hsim.assetaudit.data.entity;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;

@Entity(
        tableName = "diagnostic_log",
        indices = @Index(value = {"timestamp_utc", "level"})
)
public class DiagnosticLogEntity {
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "log_id")
    public long logId;

    @ColumnInfo(name = "timestamp_utc")
    public long timestampUtc;

    @ColumnInfo(name = "level")
    public String level;

    @ColumnInfo(name = "component")
    public String component;

    @ColumnInfo(name = "message")
    public String message;

    @ColumnInfo(name = "throwable_summary")
    public String throwableSummary;

    public DiagnosticLogEntity(long timestampUtc, String level, String component,
                               String message, String throwableSummary) {
        this.timestampUtc = timestampUtc;
        this.level = level;
        this.component = component;
        this.message = message;
        this.throwableSummary = throwableSummary;
    }
}
