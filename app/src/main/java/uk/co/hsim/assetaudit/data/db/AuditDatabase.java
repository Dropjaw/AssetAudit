package uk.co.hsim.assetaudit.data.db;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.TypeConverters;

import uk.co.hsim.assetaudit.data.dao.AppSettingDao;
import uk.co.hsim.assetaudit.data.dao.AssetDao;
import uk.co.hsim.assetaudit.data.dao.AuditEventDao;
import uk.co.hsim.assetaudit.data.dao.AuditSessionDao;
import uk.co.hsim.assetaudit.data.dao.DepartmentAuditDao;
import uk.co.hsim.assetaudit.data.dao.DiagnosticLogDao;
import uk.co.hsim.assetaudit.data.dao.ImportIssueDao;
import uk.co.hsim.assetaudit.data.dao.LookupValueDao;
import uk.co.hsim.assetaudit.data.entity.AppSettingEntity;
import uk.co.hsim.assetaudit.data.entity.AssetEntity;
import uk.co.hsim.assetaudit.data.entity.AuditEventEntity;
import uk.co.hsim.assetaudit.data.entity.AuditSessionEntity;
import uk.co.hsim.assetaudit.data.entity.DepartmentAuditEntity;
import uk.co.hsim.assetaudit.data.entity.DiagnosticLogEntity;
import uk.co.hsim.assetaudit.data.entity.ImportIssueEntity;
import uk.co.hsim.assetaudit.data.entity.LookupValueEntity;

@Database(
        entities = {
                AuditSessionEntity.class,
                AssetEntity.class,
                AuditEventEntity.class,
                DepartmentAuditEntity.class,
                ImportIssueEntity.class,
                LookupValueEntity.class,
                AppSettingEntity.class,
                DiagnosticLogEntity.class
        },
        version = 1,
        exportSchema = true
)
@TypeConverters({AuditTypeConverters.class})
public abstract class AuditDatabase extends RoomDatabase {
    public static final String DATABASE_NAME = "asset_audit.db";
    public static final int DATABASE_VERSION = 1;

    public abstract AuditSessionDao auditSessionDao();

    public abstract AssetDao assetDao();

    public abstract AuditEventDao auditEventDao();

    public abstract DepartmentAuditDao departmentAuditDao();

    public abstract ImportIssueDao importIssueDao();

    public abstract LookupValueDao lookupValueDao();

    public abstract AppSettingDao appSettingDao();

    public abstract DiagnosticLogDao diagnosticLogDao();

    public static AuditDatabase create(Context context) {
        return Room.databaseBuilder(context.getApplicationContext(), AuditDatabase.class, DATABASE_NAME)
                .setJournalMode(JournalMode.WRITE_AHEAD_LOGGING)
                .build();
    }
}
