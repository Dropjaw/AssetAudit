package uk.co.hsim.assetaudit.data.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import java.util.List;

import uk.co.hsim.assetaudit.data.entity.ImportIssueEntity;
import uk.co.hsim.assetaudit.domain.enums.ImportIssueSeverity;

@Dao
public interface ImportIssueDao {
    @Insert(onConflict = OnConflictStrategy.ABORT)
    List<Long> insertAll(List<ImportIssueEntity> entities);

    @Query("SELECT * FROM import_issue WHERE session_id = :sessionId ORDER BY severity DESC, row_number ASC")
    List<ImportIssueEntity> listBySession(String sessionId);

    @Query("SELECT COUNT(*) FROM import_issue WHERE session_id = :sessionId AND severity = :severity")
    int countBySeverity(String sessionId, ImportIssueSeverity severity);
}
