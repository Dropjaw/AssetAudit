package uk.co.hsim.assetaudit.data.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import java.util.List;

import uk.co.hsim.assetaudit.data.entity.ExportFileEntity;
import uk.co.hsim.assetaudit.data.entity.ExportRunEntity;

@Dao
public interface ExportRunDao {
    @Insert(onConflict = OnConflictStrategy.ABORT)
    void insertRun(ExportRunEntity run);

    @Insert(onConflict = OnConflictStrategy.ABORT)
    void insertFiles(List<ExportFileEntity> files);

    @Query("SELECT * FROM export_run WHERE session_id = :sessionId ORDER BY exported_at_utc DESC LIMIT :limit")
    List<ExportRunEntity> listRecentRuns(String sessionId, int limit);

    @Query("SELECT COUNT(*) FROM export_file WHERE export_run_id = :exportRunId")
    int countFiles(String exportRunId);

    @Query("SELECT COUNT(*) FROM export_run WHERE session_id = :sessionId AND export_mode = 'FINAL'")
    int countFinalExports(String sessionId);
}
