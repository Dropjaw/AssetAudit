package uk.co.hsim.assetaudit.data.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import java.util.List;

import uk.co.hsim.assetaudit.data.entity.AppSettingEntity;

@Dao
public interface AppSettingDao {
    @Query("SELECT * FROM app_setting WHERE setting_key = :key LIMIT 1")
    AppSettingEntity get(String key);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void put(AppSettingEntity entity);

    @Query("SELECT * FROM app_setting ORDER BY setting_key")
    List<AppSettingEntity> listAll();
}
