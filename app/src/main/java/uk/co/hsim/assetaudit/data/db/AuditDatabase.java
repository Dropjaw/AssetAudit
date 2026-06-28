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
import uk.co.hsim.assetaudit.data.dao.ExportRunDao;
import uk.co.hsim.assetaudit.data.dao.ImportIssueDao;
import uk.co.hsim.assetaudit.data.dao.LookupValueDao;
import uk.co.hsim.assetaudit.data.entity.AppSettingEntity;
import uk.co.hsim.assetaudit.data.entity.AssetEntity;
import uk.co.hsim.assetaudit.data.entity.AuditEventEntity;
import uk.co.hsim.assetaudit.data.entity.AuditSessionEntity;
import uk.co.hsim.assetaudit.data.entity.DepartmentAuditEntity;
import uk.co.hsim.assetaudit.data.entity.DiagnosticLogEntity;
import uk.co.hsim.assetaudit.data.entity.ExportFileEntity;
import uk.co.hsim.assetaudit.data.entity.ExportRunEntity;
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
                DiagnosticLogEntity.class,
                ExportRunEntity.class,
                ExportFileEntity.class
        },
        version = 3,
        exportSchema = true
)
@TypeConverters({AuditTypeConverters.class})
public abstract class AuditDatabase extends RoomDatabase {
    public static final String DATABASE_NAME = "asset_audit.db";
    public static final int DATABASE_VERSION = 3;

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

    public static final Migration MIGRATION_2_3 = new Migration(2, 3) {
        @Override
        public void migrate(SupportSQLiteDatabase database) {
            database.execSQL("CREATE TABLE IF NOT EXISTS export_run (export_run_id TEXT NOT NULL, session_id TEXT NOT NULL, package_id TEXT, destination_display_name TEXT, export_mode TEXT, readiness TEXT, exported_at_utc INTEGER NOT NULL, app_version_name TEXT, device_id TEXT, user_name TEXT, manifest_json TEXT, PRIMARY KEY(export_run_id), FOREIGN KEY(session_id) REFERENCES audit_session(session_id) ON UPDATE NO ACTION ON DELETE CASCADE)");
            database.execSQL("CREATE INDEX IF NOT EXISTS index_export_run_session_id_exported_at_utc ON export_run(session_id, exported_at_utc)");
            database.execSQL("CREATE TABLE IF NOT EXISTS export_file (export_file_id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, export_run_id TEXT NOT NULL, file_name TEXT, media_type TEXT, row_count INTEGER NOT NULL, sha256 TEXT, FOREIGN KEY(export_run_id) REFERENCES export_run(export_run_id) ON UPDATE NO ACTION ON DELETE CASCADE)");
            database.execSQL("CREATE INDEX IF NOT EXISTS index_export_file_export_run_id ON export_file(export_run_id)");
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

    public abstract ExportRunDao exportRunDao();

    public static AuditDatabase create(Context context) {
        return Room.databaseBuilder(context.getApplicationContext(), AuditDatabase.class, DATABASE_NAME)
                .setJournalMode(JournalMode.WRITE_AHEAD_LOGGING)
                .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
                .build();
    }
}
