package uk.co.hsim.assetaudit.data.entity;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "app_setting")
public class AppSettingEntity {
    @PrimaryKey
    @NonNull
    @ColumnInfo(name = "setting_key")
    public String settingKey;

    @NonNull
    @ColumnInfo(name = "setting_value")
    public String settingValue;

    @ColumnInfo(name = "updated_at_utc")
    public long updatedAtUtc;

    public AppSettingEntity(@NonNull String settingKey, @NonNull String settingValue,
                            long updatedAtUtc) {
        this.settingKey = settingKey;
        this.settingValue = settingValue;
        this.updatedAtUtc = updatedAtUtc;
    }
}
