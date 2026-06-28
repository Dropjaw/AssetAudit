package uk.co.hsim.assetaudit.data.entity;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.PrimaryKey;

@Entity(
        tableName = "export_file",
        foreignKeys = @ForeignKey(
                entity = ExportRunEntity.class,
                parentColumns = "export_run_id",
                childColumns = "export_run_id",
                onDelete = ForeignKey.CASCADE
        ),
        indices = @Index("export_run_id")
)
public class ExportFileEntity {
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "export_file_id")
    public long exportFileId;

    @NonNull
    @ColumnInfo(name = "export_run_id")
    public String exportRunId;

    @ColumnInfo(name = "file_name")
    public String fileName;

    @ColumnInfo(name = "media_type")
    public String mediaType;

    @ColumnInfo(name = "row_count")
    public int rowCount;

    @ColumnInfo(name = "sha256")
    public String sha256;

    public ExportFileEntity(@NonNull String exportRunId, String fileName, String mediaType,
                            int rowCount, String sha256) {
        this.exportRunId = exportRunId;
        this.fileName = fileName;
        this.mediaType = mediaType;
        this.rowCount = rowCount;
        this.sha256 = sha256;
    }
}
