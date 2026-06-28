package uk.co.hsim.assetaudit.data;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
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
import java.util.Collections;

import uk.co.hsim.assetaudit.data.db.AuditDatabase;
import uk.co.hsim.assetaudit.data.entity.AssetEntity;
import uk.co.hsim.assetaudit.data.entity.DepartmentAuditEntity;
import uk.co.hsim.assetaudit.data.repository.RoomAssetRepository;
import uk.co.hsim.assetaudit.data.repository.RoomDepartmentAuditRepository;
import uk.co.hsim.assetaudit.data.repository.RoomDiagnosticRepository;
import uk.co.hsim.assetaudit.domain.enums.AuditStatus;
import uk.co.hsim.assetaudit.domain.enums.DepartmentAuditStatus;
import uk.co.hsim.assetaudit.domain.enums.EventKind;
import uk.co.hsim.assetaudit.domain.results.OperationResult;
import uk.co.hsim.assetaudit.service.DepartmentSummaryService;
import uk.co.hsim.assetaudit.service.DiagnosticService;
import uk.co.hsim.assetaudit.service.DuplicateReviewState;
import uk.co.hsim.assetaudit.service.ExceptionResolutionResult;
import uk.co.hsim.assetaudit.service.ExceptionResolutionService;
import uk.co.hsim.assetaudit.service.FinishDepartmentPreview;
import uk.co.hsim.assetaudit.service.MovementConfirmationRequest;
import uk.co.hsim.assetaudit.service.NewAssetDraft;
import uk.co.hsim.assetaudit.service.SkipAssetsRequest;
import uk.co.hsim.assetaudit.util.device.DeviceInfoProvider;
import uk.co.hsim.assetaudit.util.logging.AndroidAppLogger;

@RunWith(AndroidJUnit4.class)
public class ExceptionResolutionServiceTests {
    private static final String SESSION_ID = "exceptions-session";
    private AuditDatabase database;
    private ExceptionResolutionService service;
    private long now;

    @Before
    public void setUp() {
        Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        database = Room.inMemoryDatabaseBuilder(context, AuditDatabase.class)
                .allowMainThreadQueries()
                .build();
        now = 7000L;
        DepartmentSummaryService summaries = new DepartmentSummaryService(
                new RoomDepartmentAuditRepository(database.departmentAuditDao()),
                new RoomAssetRepository(database.assetDao())
        );
        service = new ExceptionResolutionService(
                database,
                summaries,
                new DiagnosticService(new RoomDiagnosticRepository(database.diagnosticLogDao()), () -> now, new AndroidAppLogger()),
                () -> now,
                () -> "tester",
                new FakeDeviceInfoProvider()
        );

        database.auditSessionDao().insert(TestEntities.session(SESSION_ID));
        database.departmentAuditDao().insert(new DepartmentAuditEntity(
                SESSION_ID, "IT", 3, 0, 0, 0, 0,
                DepartmentAuditStatus.NOT_STARTED, null));
        database.departmentAuditDao().insert(TestEntities.department(SESSION_ID, "Finance"));
        database.assetDao().insert(TestEntities.asset(SESSION_ID, "0011", "IT"));
        database.assetDao().insert(TestEntities.asset(SESSION_ID, "0069", "IT"));
        database.assetDao().insert(TestEntities.asset(SESSION_ID, "F001", "Finance"));
    }

    @After
    public void tearDown() {
        database.close();
    }

    @Test
    public void confirmMovementUpdatesAssetEventAndCountsAtomically() {
        OperationResult<ExceptionResolutionResult> result = service.confirmMovement(new MovementConfirmationRequest(
                SESSION_ID, "F001", "IT", "Finance", "Moved during audit"));

        assertTrue(result.isSuccess());
        AssetEntity moved = database.assetDao().getByTag(SESSION_ID, "F001");
        assertEquals("IT", moved.department);
        assertEquals("Finance", moved.previousDepartment);
        assertEquals(AuditStatus.AUDITED_MOVED_DEPARTMENT, moved.auditStatus);
        DepartmentAuditEntity it = database.departmentAuditDao().getDepartment(SESSION_ID, "IT");
        assertEquals(1, it.scannedCount);
        assertEquals(1, it.movedInCount);
        assertEquals(1, database.auditEventDao().countByDepartmentAndKind(SESSION_ID, "IT", EventKind.MOVEMENT_CONFIRMED));
    }

    @Test
    public void staleMovementLeavesRowsUnchanged() {
        database.assetDao().updateAuditStatus(SESSION_ID, "F001", AuditStatus.AUDITED_EXPECTED, now);

        OperationResult<ExceptionResolutionResult> result = service.confirmMovement(new MovementConfirmationRequest(
                SESSION_ID, "F001", "IT", "Finance", ""));

        assertFalse(result.isSuccess());
        assertEquals("Finance", database.assetDao().getByTag(SESSION_ID, "F001").department);
        assertEquals(0, database.departmentAuditDao().getDepartment(SESSION_ID, "IT").movedInCount);
        assertEquals(0, database.auditEventDao().countByDepartmentAndKind(SESSION_ID, "IT", EventKind.MOVEMENT_CONFIRMED));
    }

    @Test
    public void newAssetCreationInsertsAssetEventAndNewCount() {
        OperationResult<ExceptionResolutionResult> result = service.createNewAsset(validDraft("0099"));

        assertTrue(result.isSuccess());
        AssetEntity asset = database.assetDao().getByTag(SESSION_ID, "0099");
        assertNotNull(asset);
        assertEquals("0099", asset.assetTagId);
        assertEquals(AuditStatus.NEW_ASSET_ADDED, asset.auditStatus);
        assertTrue(asset.createdDuringAudit);
        assertEquals(1, database.departmentAuditDao().getDepartment(SESSION_ID, "IT").newAssetCount);
        assertEquals(1, database.auditEventDao().countByDepartmentAndKind(SESSION_ID, "IT", EventKind.NEW_ASSET_CREATED));
    }

    @Test
    public void duplicateNewAssetRollsBack() {
        OperationResult<ExceptionResolutionResult> result = service.createNewAsset(validDraft("0011"));

        assertFalse(result.isSuccess());
        assertEquals(AuditStatus.NOT_AUDITED, database.assetDao().getByTag(SESSION_ID, "0011").auditStatus);
        assertEquals(0, database.departmentAuditDao().getDepartment(SESSION_ID, "IT").newAssetCount);
    }

    @Test
    public void finishPreviewListsRemainingAssets() {
        OperationResult<FinishDepartmentPreview> result = service.previewFinishDepartment(SESSION_ID, "IT");

        assertTrue(result.isSuccess());
        assertEquals(2, result.getValue().getRemainingCount());
    }

    @Test
    public void markMissingUpdatesSelectedAssetsEventsAndCounts() {
        OperationResult<ExceptionResolutionResult> result = service.markRemainingMissing(
                SESSION_ID, "IT", Arrays.asList("0011", "0069"));

        assertTrue(result.isSuccess());
        assertEquals(AuditStatus.MISSING_AFTER_DEPARTMENT_CLOSE, database.assetDao().getByTag(SESSION_ID, "0011").auditStatus);
        assertEquals(AuditStatus.MISSING_AFTER_DEPARTMENT_CLOSE, database.assetDao().getByTag(SESSION_ID, "0069").auditStatus);
        DepartmentAuditEntity department = database.departmentAuditDao().getDepartment(SESSION_ID, "IT");
        assertEquals(2, department.missingCount);
        assertEquals(DepartmentAuditStatus.COMPLETE_WITH_EXCEPTIONS, department.status);
        assertEquals(2, database.auditEventDao().countByDepartmentAndKind(SESSION_ID, "IT", EventKind.ASSET_MARKED_MISSING));
    }

    @Test
    public void staleMissingRollsBackWithoutPartialMutation() {
        database.assetDao().updateAuditStatus(SESSION_ID, "0069", AuditStatus.AUDITED_EXPECTED, now);

        OperationResult<ExceptionResolutionResult> result = service.markRemainingMissing(
                SESSION_ID, "IT", Arrays.asList("0011", "0069"));

        assertFalse(result.isSuccess());
        assertEquals(AuditStatus.NOT_AUDITED, database.assetDao().getByTag(SESSION_ID, "0011").auditStatus);
        assertEquals(0, database.departmentAuditDao().getDepartment(SESSION_ID, "IT").missingCount);
    }

    @Test
    public void skipAssetsRequiresReasonAndWritesEvents() {
        OperationResult<ExceptionResolutionResult> blank = service.skipAssets(new SkipAssetsRequest(
                SESSION_ID, "IT", Collections.singletonList("0011"), " "));
        assertFalse(blank.isSuccess());

        OperationResult<ExceptionResolutionResult> result = service.skipAssets(new SkipAssetsRequest(
                SESSION_ID, "IT", Collections.singletonList("0011"), "locked office"));

        assertTrue(result.isSuccess());
        assertEquals(AuditStatus.SKIPPED_UNABLE_TO_VERIFY, database.assetDao().getByTag(SESSION_ID, "0011").auditStatus);
        assertEquals(1, database.auditEventDao().countByDepartmentAndKind(SESSION_ID, "IT", EventKind.ASSET_SKIPPED));
    }

    @Test
    public void duplicateReviewDoesNotMutate() {
        database.assetDao().updateAuditStatus(SESSION_ID, "0011", AuditStatus.AUDITED_EXPECTED, now);

        OperationResult<DuplicateReviewState> result = service.buildDuplicateReview(SESSION_ID, "0011");

        assertTrue(result.isSuccess());
        assertEquals(AuditStatus.AUDITED_EXPECTED, result.getValue().getAsset().auditStatus);
        assertEquals(0, database.departmentAuditDao().getDepartment(SESSION_ID, "IT").scannedCount);
    }

    private NewAssetDraft validDraft(String tag) {
        return new NewAssetDraft(
                SESSION_ID,
                tag,
                "IT",
                "Docking station",
                "Available",
                "Main",
                "Unit 11",
                "Computer equipment",
                "Dock",
                "",
                "",
                "added in test"
        );
    }

    private static final class FakeDeviceInfoProvider implements DeviceInfoProvider {
        @Override
        public String getManufacturer() {
            return "Zebra";
        }

        @Override
        public String getModel() {
            return "TC21";
        }

        @Override
        public String getAndroidRelease() {
            return "test";
        }

        @Override
        public int getSdkInt() {
            return 34;
        }

        @Override
        public String getPackageName() {
            return "uk.co.hsim.assetaudit";
        }

        @Override
        public String getMainActivityName() {
            return "MainActivity";
        }

        @Override
        public String getAppVersionName() {
            return "test";
        }

        @Override
        public long getAppVersionCode() {
            return 1L;
        }
    }
}
