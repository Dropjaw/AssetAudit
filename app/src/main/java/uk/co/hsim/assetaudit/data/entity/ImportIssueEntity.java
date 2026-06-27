package uk.co.hsim.assetaudit.data.entity;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Ignore;
import androidx.room.Index;
import androidx.room.PrimaryKey;

import uk.co.hsim.assetaudit.domain.enums.ImportIssueSeverity;

@Entity(
        tableName = "import_issue",
        foreignKeys = @ForeignKey(
                entity = AuditSessionEntity.class,
                parentColumns = "session_id",
                childColumns = "session_id",
                onDelete = ForeignKey.CASCADE
        ),
        indices = @Index(value = {"session_id", "severity"})
)
public class ImportIssueEntity {
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "issue_id")
    public long issueId;

    @NonNull
    @ColumnInfo(name = "session_id")
    public String sessionId;

    @ColumnInfo(name = "row_number")
    public Integer rowNumber;

    @ColumnInfo(name = "column_name")
    public String columnName;

    @NonNull
    @ColumnInfo(name = "severity")
    public ImportIssueSeverity severity;

    @ColumnInfo(name = "issue_code")
    public String issueCode;

    @ColumnInfo(name = "message")
    public String message;

    @ColumnInfo(name = "source_value")
    public String sourceValue;

    @ColumnInfo(name = "resolved")
    public boolean resolved;

    public ImportIssueEntity() {
    }

    @Ignore
    public ImportIssueEntity(@NonNull String sessionId, Integer rowNumber, String columnName,
                             @NonNull ImportIssueSeverity severity, String message) {
        this.sessionId = sessionId;
        this.rowNumber = rowNumber;
        this.columnName = columnName;
        this.severity = severity;
        this.issueCode = null;
        this.message = message;
        this.sourceValue = null;
        this.resolved = false;
    }

    @Ignore
    public ImportIssueEntity(@NonNull String sessionId, Integer rowNumber, String columnName,
                             @NonNull ImportIssueSeverity severity, String issueCode,
                             String message, String sourceValue, boolean resolved) {
        this.sessionId = sessionId;
        this.rowNumber = rowNumber;
        this.columnName = columnName;
        this.severity = severity;
        this.issueCode = issueCode;
        this.message = message;
        this.sourceValue = sourceValue;
        this.resolved = resolved;
    }
}
