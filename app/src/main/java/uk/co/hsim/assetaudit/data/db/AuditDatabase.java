package uk.co.hsim.assetaudit.data.db;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.TypeConverters;
import androidx.room.migration.Migration;
import androidx.sqlite.db.SupportSQLiteDatabase;

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
        version = 2,
        exportSchema = true
)
@TypeConverters({AuditTypeConverters.class})
public abstract class AuditDatabase extends RoomDatabase {
    public static final String DATABASE_NAME = "asset_audit.db";
    public static final int DATABASE_VERSION = 2;

    public static final Migration MIGRATION_1_2 = new Migration(1, 2) {
        @Override
        public void migrate(SupportSQLiteDatabase database) {
            database.execSQL("ALTER TABLE audit_session ADD COLUMN source_format TEXT");
            database.execSQL("ALTER TABLE audit_session ADD COLUMN source_row_count INTEGER");
            database.execSQL("ALTER TABLE audit_session ADD COLUMN imported_at_utc INTEGER");
            database.execSQL("ALTER TABLE asset ADD COLUMN source_row_number INTEGER");
            database.execSQL("ALTER TABLE asset ADD COLUMN brand TEXT");
            database.execSQL("ALTER TABLE asset ADD COLUMN model TEXT");
            database.execSQL("ALTER TABLE asset ADD COLUMN audit_details TEXT");
            database.execSQL("ALTER TABLE asset ADD COLUMN imported_at_utc INTEGER");
            database.execSQL("ALTER TABLE import_issue ADD COLUMN issue_code TEXT");
            database.execSQL("ALTER TABLE import_issue ADD COLUMN source_value TEXT");
            database.execSQL("ALTER TABLE import_issue ADD COLUMN resolved INTEGER NOT NULL DEFAULT 0");
        }
    };

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
                .addMigrations(MIGRATION_1_2)
                .build();
    }
}
