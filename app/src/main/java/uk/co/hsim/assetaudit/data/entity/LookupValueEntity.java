package uk.co.hsim.assetaudit.data.entity;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;

@Entity(
        tableName = "lookup_value",
        indices = @Index(value = {"lookup_type", "value"}, unique = true)
)
public class LookupValueEntity {
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "lookup_id")
    public long lookupId;

    @NonNull
    @ColumnInfo(name = "lookup_type")
    public String lookupType;

    @NonNull
    @ColumnInfo(name = "value")
    public String value;

    @ColumnInfo(name = "display_order")
    public int displayOrder;

    @ColumnInfo(name = "active")
    public boolean active;

    public LookupValueEntity(@NonNull String lookupType, @NonNull String value,
                             int displayOrder, boolean active) {
        this.lookupType = lookupType;
        this.value = value;
        this.displayOrder = displayOrder;
        this.active = active;
    }
}
