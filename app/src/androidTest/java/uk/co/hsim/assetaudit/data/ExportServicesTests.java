package uk.co.hsim.assetaudit.data;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import android.content.Context;

import androidx.room.Room;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Arrays;
import java.util.List;

import uk.co.hsim.assetaudit.data.db.AuditDatabase;
import uk.co.hsim.assetaudit.data.entity.AuditEventEntity;
import uk.co.hsim.assetaudit.data.entity.ExportRunEntity;
import uk.co.hsim.assetaudit.domain.enums.AuditStatus;
import uk.co.hsim.assetaudit.domain.enums.DepartmentAuditStatus;
import uk.co.hsim.assetaudit.domain.enums.EventKind;
import uk.co.hsim.assetaudit.domain.results.OperationResult;
import uk.co.hsim.assetaudit.export.ExportCompletionService;
import uk.co.hsim.assetaudit.export.ExportDestinationSummary;
import uk.co.hsim.assetaudit.export.ExportFileRecord;
import uk.co.hsim.assetaudit.export.ExportMode;
import uk.co.hsim.assetaudit.export.ExportOptions;
import uk.co.hsim.assetaudit.export.ExportPackageResult;
import uk.co.hsim.assetaudit.export.ExportPreview;
import uk.co.hsim.assetaudit.export.ExportReadinessLevel;
import uk.co.hsim.assetaudit.export.ExportSnapshot;
import uk.co.hsim.assetaudit.export.ExportSnapshotBuilder;
import uk.co.hsim.assetaudit.export.ReportPreviewService;
import uk.co.hsim.assetaudit.util.device.DeviceInfoProvider;

@RunWith(AndroidJUnit4.class)
public class ExportServicesTests {
    private static final String SESSION_ID = "export-session";
    private AuditDatabase database;
    private ReportPreviewService previewService;
    private ExportSnapshotBuilder snapshotBuilder;
    private ExportCompletionService completionService;

    @Before
    public void setUp() {
        Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        database = Room.inMemoryDatabaseBuilder(context, AuditDatabase.class)
                .allowMainThreadQueries()
                .build();
        previewService = new ReportPreviewService(database, () -> 9000L);
        snapshotBuilder = new ExportSnapshotBuilder(database, previewService, () -> 9100L);
        completionService = new ExportCompletionService(database, new FakeDeviceInfoProvider(), () -> "tester");

        database.auditSessionDao().insert(TestEntities.session(SESSION_ID));
        database.departmentAuditDao().insert(TestEntities.department(SESSION_ID, "IT"));
        database.assetDao().insert(TestEntities.asset(SESSION_ID, "0011", "IT"));
    }

    @After
    public void tearDown() {
        database.close();
    }

    @Test
    public void previewBlocksFinalExportButAllowsDraftForRemainingAssets() {
        OperationResult<ExportPreview> finalPreview = previewService.buildPreview(SESSION_ID, false);
        OperationResult<ExportPreview> draftPreview = previewService.buildPreview(SESSION_ID, true);

        assertTrue(finalPreview.isSuccess());
        assertEquals(ExportReadinessLevel.BLOCKED, finalPreview.getValue().getReadiness());
        assertTrue(draftPreview.isSuccess());
        assertEquals(ExportReadinessLevel.DRAFT_INCOMPLETE, draftPreview.getValue().getReadiness());
        assertEquals(1, draftPreview.getValue().getRemainingAssets());
    }

    @Test
    public void completionRecordsExportRunFilesAndSessionEvent() {
        database.assetDao().updateAuditStatus(SESSION_ID, "0011", AuditStatus.AUDITED_EXPECTED, 9200L);
        database.departmentAuditDao().updateCounts(
                SESSION_ID, "IT", 1, 0, 0, 0, DepartmentAuditStatus.COMPLETE, 9300L);
        OperationResult<ExportSnapshot> snapshot = snapshotBuilder.build(SESSION_ID, false);
        assertTrue(snapshot.isSuccess());

        ExportPackageResult packageResult = new ExportPackageResult(
                snapshot.getValue().packageId,
                "{\"packageId\":\"" + snapshot.getValue().packageId + "\"}",
                Arrays.asList(
                        new ExportFileRecord("updated_assets.csv", "text/csv", 1,
                                "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa"),
                        new ExportFileRecord("export_manifest.json", "application/json", 1,
                                "bbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbb")
                )
        );
        OperationResult<String> result = completionService.recordSuccess(
                snapshot.getValue(),
                ExportOptions.defaults(ExportMode.FINAL),
                packageResult,
                new ExportDestinationSummary("export.zip", "export.zip (destination #abcd1234)")
        );

        assertTrue(result.isSuccess());
        List<ExportRunEntity> runs = database.exportRunDao().listRecentRuns(SESSION_ID, 5);
        assertEquals(1, runs.size());
        assertEquals("export.zip (destination #abcd1234)", runs.get(0).destinationDisplayName);
        assertEquals(2, database.exportRunDao().countFiles(runs.get(0).exportRunId));
        assertEquals(1, countSessionExportedEvents());
    }

    private int countSessionExportedEvents() {
        int count = 0;
        for (AuditEventEntity event : database.auditEventDao().listBySession(SESSION_ID)) {
            if (event.eventKind == EventKind.SESSION_EXPORTED) {
                count++;
            }
        }
        return count;
    }

    private static final class FakeDeviceInfoProvider implements DeviceInfoProvider {
        @Override
        public String getManufacturer() { return "Zebra"; }

        @Override
        public String getModel() { return "TC21"; }

        @Override
        public String getAndroidRelease() { return "test"; }

        @Override
        public int getSdkInt() { return 34; }

        @Override
        public String getPackageName() { return "uk.co.hsim.assetaudit"; }

        @Override
        public String getMainActivityName() { return "MainActivity"; }

        @Override
        public String getAppVersionName() { return "test"; }

        @Override
        public long getAppVersionCode() { return 1L; }
    }
}
