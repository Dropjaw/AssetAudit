package uk.co.hsim.assetaudit.data.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import java.util.List;

import uk.co.hsim.assetaudit.data.entity.DiagnosticLogEntity;

@Dao
public interface DiagnosticLogDao {
    @Insert(onConflict = OnConflictStrategy.ABORT)
    long insert(DiagnosticLogEntity entity);

    @Query("SELECT * FROM diagnostic_log ORDER BY timestamp_utc DESC LIMIT :limit")
    List<DiagnosticLogEntity> listRecent(int limit);

    @Query("DELETE FROM diagnostic_log WHERE timestamp_utc < :thresholdUtc")
    int deleteOlderThan(long thresholdUtc);
}
