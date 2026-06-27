package uk.co.hsim.assetaudit.data.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import java.util.List;

import uk.co.hsim.assetaudit.data.entity.LookupValueEntity;

@Dao
public interface LookupValueDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    List<Long> insertAll(List<LookupValueEntity> entities);

    @Query("SELECT * FROM lookup_value WHERE lookup_type = :lookupType AND active = 1 ORDER BY display_order ASC, value ASC")
    List<LookupValueEntity> listActiveByType(String lookupType);
}
