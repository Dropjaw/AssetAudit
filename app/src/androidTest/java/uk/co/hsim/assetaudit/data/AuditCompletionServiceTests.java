package uk.co.hsim.assetaudit.data;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import android.content.Context;

import androidx.room.Room;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Collections;

import uk.co.hsim.assetaudit.data.db.AuditDatabase;
import uk.co.hsim.assetaudit.data.entity.AuditEventEntity;
import uk.co.hsim.assetaudit.data.entity.ExportFileEntity;
import uk.co.hsim.assetaudit.data.entity.ExportRunEntity;
import uk.co.hsim.assetaudit.data.repository.RoomDiagnosticRepository;
import uk.co.hsim.assetaudit.domain.enums.AuditStatus;
import uk.co.hsim.assetaudit.domain.enums.DepartmentAuditStatus;
import uk.co.hsim.assetaudit.domain.enums.EventKind;
import uk.co.hsim.assetaudit.domain.enums.SessionStatus;
import uk.co.hsim.assetaudit.domain.results.OperationResult;
import uk.co.hsim.assetaudit.service.AuditCompletionResult;
import uk.co.hsim.assetaudit.service.AuditCompletionService;
import uk.co.hsim.assetaudit.service.DiagnosticService;
import uk.co.hsim.assetaudit.service.ForceCloseAuditRequest;
import uk.co.hsim.assetaudit.util.device.DeviceInfoProvider;
import uk.co.hsim.assetaudit.util.logging.AndroidAppLogger;

@RunWith(AndroidJUnit4.class)
public class AuditCompletionServiceTests {
    private static final String SESSION_ID = "completion-session";
    private AuditDatabase database;
    private AuditCompletionService service;

    @Before
    public void setUp() {
        Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        database = Room.inMemoryDatabaseBuilder(context, AuditDatabase.class)
                .allowMainThreadQueries()
                .build();
        service = new AuditCompletionService(
                database,
                new DiagnosticService(new RoomDiagnosticRepository(database.diagnosticLogDao()),
                        () -> 9000L, new AndroidAppLogger()),
                () -> 9000L,
                () -> "tester",
                new FakeDeviceInfoProvider()
        );

        database.auditSessionDao().insert(TestEntities.session(SESSION_ID));
        database.departmentAuditDao().insert(TestEntities.department(SESSION_ID, "IT"));
        database.assetDao().insert(TestEntities.asset(SESSION_ID, "0011", "IT"));
    }

    @After
    public void tearDown() {
        database.close();
    }

    @Test
    public void completeDepartmentsWithoutFinalExportRemainActive() {
        completeDepartmentAndAsset();

        OperationResult<AuditCompletionResult> result = service.tryCompleteAfterFinalExport(SESSION_ID, "", "");

        assertTrue(result.isSuccess());
        assertFalse(result.getValue().isChanged());
        assertEquals(SessionStatus.ACTIVE, database.auditSessionDao().getById(SESSION_ID).status);
        assertEquals(0, database.auditEventDao().countBySessionAndKind(SESSION_ID, EventKind.SESSION_COMPLETED));
    }

    @Test
    public void draftExportNeverCompletesAudit() {
        completeDepartmentAndAsset();
        insertExport("DRAFT", "draft-package");
        insertSessionExportedEvent("draft-package");

        OperationResult<AuditCompletionResult> result = service.tryCompleteAfterFinalExport(SESSION_ID, "draft-run", "draft-package");

        assertTrue(result.isSuccess());
        assertFalse(result.getValue().isChanged());
        assertEquals(SessionStatus.ACTIVE, database.auditSessionDao().getById(SESSION_ID).status);
    }

    @Test
    public void finalExportCompletesAuditAndIsIdempotent() {
        completeDepartmentAndAsset();
        insertExport("FINAL", "final-package");
        insertSessionExportedEvent("final-package");

        OperationResult<AuditCompletionResult> first = service.tryCompleteAfterFinalExport(SESSION_ID, "final-run", "final-package");
        OperationResult<AuditCompletionResult> second = service.tryCompleteAfterFinalExport(SESSION_ID, "final-run", "final-package");

        assertTrue(first.isSuccess());
        assertTrue(first.getValue().isChanged());
        assertTrue(second.isSuccess());
        assertFalse(second.getValue().isChanged());
        assertEquals(SessionStatus.COMPLETED, database.auditSessionDao().getById(SESSION_ID).status);
        assertEquals(1, database.auditEventDao().countBySessionAndKind(SESSION_ID, EventKind.SESSION_COMPLETED));
    }

    @Test
    public void finalExportWithIncompleteDepartmentDoesNotComplete() {
        insertExport("FINAL", "final-package");
        insertSessionExportedEvent("final-package");

        OperationResult<AuditCompletionResult> result = service.tryCompleteAfterFinalExport(SESSION_ID, "final-run", "final-package");

        assertTrue(result.isSuccess());
        assertFalse(result.getValue().isChanged());
        assertEquals(SessionStatus.ACTIVE, database.auditSessionDao().getById(SESSION_ID).status);
        assertEquals(0, database.auditEventDao().countBySessionAndKind(SESSION_ID, EventKind.SESSION_COMPLETED));
    }

    @Test
    public void forceCloseRequiresReasonAndPreservesAssetStatuses() {
        OperationResult<AuditCompletionResult> blank = service.forceCloseCurrentAudit(
                new ForceCloseAuditRequest(SESSION_ID, " ", true));
        assertFalse(blank.isSuccess());

        OperationResult<AuditCompletionResult> result = service.forceCloseCurrentAudit(
                new ForceCloseAuditRequest(SESSION_ID, "stocktake stopped", true));

        assertTrue(result.isSuccess());
        assertTrue(result.getValue().isChanged());
        assertEquals(SessionStatus.FORCE_CLOSED, database.auditSessionDao().getById(SESSION_ID).status);
        assertEquals(AuditStatus.NOT_AUDITED, database.assetDao().getByTag(SESSION_ID, "0011").auditStatus);
        assertEquals(1, database.auditEventDao().countBySessionAndKind(SESSION_ID, EventKind.SESSION_FORCE_CLOSED));
    }

    @Test
    public void forceClosedAuditCannotBecomeCompleted() {
        assertTrue(service.forceCloseCurrentAudit(
                new ForceCloseAuditRequest(SESSION_ID, "closed", true)).isSuccess());
        completeDepartmentAndAsset();
        insertExport("FINAL", "final-package");
        insertSessionExportedEvent("final-package");

        OperationResult<AuditCompletionResult> result = service.tryCompleteAfterFinalExport(SESSION_ID, "final-run", "final-package");

        assertTrue(result.isSuccess());
        assertFalse(result.getValue().isChanged());
        assertEquals(SessionStatus.FORCE_CLOSED, database.auditSessionDao().getById(SESSION_ID).status);
    }

    private void completeDepartmentAndAsset() {
        database.assetDao().updateAuditStatus(SESSION_ID, "0011", AuditStatus.AUDITED_EXPECTED, 8000L);
        database.departmentAuditDao().updateCounts(
                SESSION_ID, "IT", 1, 0, 0, 0, DepartmentAuditStatus.COMPLETE, 8100L);
    }

    private void insertExport(String mode, String packageId) {
        String runId = mode.toLowerCase() + "-run";
        database.exportRunDao().insertRun(new ExportRunEntity(
                runId,
                SESSION_ID,
                packageId,
                "export.zip",
                mode,
                "READY",
                8200L,
                "test",
                "TC21",
                "tester",
                "{}"
        ));
        database.exportRunDao().insertFiles(Collections.singletonList(new ExportFileEntity(
                runId,
                "updated_assets.csv",
                "text/csv",
                1,
                "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa"
        )));
    }

    private void insertSessionExportedEvent(String packageId) {
        database.auditEventDao().insert(new AuditEventEntity(
                packageId + "-event",
                SESSION_ID,
                null,
                EventKind.SESSION_EXPORTED,
                null,
                null,
                null,
                8300L,
                "tester",
                "TC21",
                "Export package " + packageId
        ));
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
