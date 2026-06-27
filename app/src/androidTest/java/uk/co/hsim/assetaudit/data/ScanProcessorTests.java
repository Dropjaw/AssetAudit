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

import uk.co.hsim.assetaudit.data.db.AuditDatabase;
import uk.co.hsim.assetaudit.data.entity.AssetEntity;
import uk.co.hsim.assetaudit.data.entity.DepartmentAuditEntity;
import uk.co.hsim.assetaudit.domain.enums.AuditStatus;
import uk.co.hsim.assetaudit.domain.enums.DepartmentAuditStatus;
import uk.co.hsim.assetaudit.domain.enums.ScanResultType;
import uk.co.hsim.assetaudit.service.ScanProcessingResult;
import uk.co.hsim.assetaudit.service.ScanProcessor;
import uk.co.hsim.assetaudit.service.ScanRequest;
import uk.co.hsim.assetaudit.util.device.DeviceInfoProvider;

@RunWith(AndroidJUnit4.class)
public class ScanProcessorTests {
    private static final String SESSION_ID = "scan-session";
    private AuditDatabase database;
    private ScanProcessor processor;
    private long now;

    @Before
    public void setUp() {
        Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        database = Room.inMemoryDatabaseBuilder(context, AuditDatabase.class)
                .allowMainThreadQueries()
                .build();
        now = 5000L;
        processor = new ScanProcessor(database, () -> now, () -> "tester", new FakeDeviceInfoProvider());

        database.auditSessionDao().insert(TestEntities.session(SESSION_ID));
        database.departmentAuditDao().insert(new DepartmentAuditEntity(
                SESSION_ID,
                "IT",
                2,
                0,
                0,
                0,
                0,
                DepartmentAuditStatus.NOT_STARTED,
                null
        ));
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
    public void expectedScanUpdatesAssetDepartmentAndEventAtomically() {
        ScanProcessingResult result = processor.processScan(request(" 0011 "));

        assertTrue(result.isSuccess());
        assertEquals(ScanResultType.SUCCESS_EXPECTED, result.getResultType());
        AssetEntity asset = database.assetDao().getByTag(SESSION_ID, "0011");
        assertEquals(AuditStatus.AUDITED_EXPECTED, asset.auditStatus);
        DepartmentAuditEntity department = database.departmentAuditDao().getDepartment(SESSION_ID, "IT");
        assertEquals(1, department.scannedCount);
        assertEquals(DepartmentAuditStatus.IN_PROGRESS, department.status);
        assertEquals(1, database.auditEventDao().countBySession(SESSION_ID));
    }

    @Test
    public void finalExpectedScanCompletesDepartment() {
        processor.processScan(request("0011"));
        now = 6000L;
        ScanProcessingResult result = processor.processScan(request("0069"));

        assertTrue(result.isSuccess());
        DepartmentAuditEntity department = database.departmentAuditDao().getDepartment(SESSION_ID, "IT");
        assertEquals(2, department.scannedCount);
        assertEquals(DepartmentAuditStatus.COMPLETE, department.status);
        assertEquals(Long.valueOf(6000L), department.completedAtUtc);
    }

    @Test
    public void duplicateScanDoesNotIncrementProgressAgain() {
        processor.processScan(request("0011"));
        now = 6000L;
        ScanProcessingResult duplicate = processor.processScan(request("0011"));

        assertFalse(duplicate.isSuccess());
        assertEquals(ScanResultType.DUPLICATE_SCAN, duplicate.getResultType());
        DepartmentAuditEntity department = database.departmentAuditDao().getDepartment(SESSION_ID, "IT");
        assertEquals(1, department.scannedCount);
        assertEquals(AuditStatus.AUDITED_EXPECTED, database.assetDao().getByTag(SESSION_ID, "0011").auditStatus);
        assertEquals(2, database.auditEventDao().countBySession(SESSION_ID));
    }

    @Test
    public void wrongDepartmentScanDoesNotMutateAssetOrProgress() {
        ScanProcessingResult result = processor.processScan(request("F001"));

        assertFalse(result.isSuccess());
        assertEquals(ScanResultType.FOUND_IN_OTHER_DEPARTMENT_REQUIRES_CONFIRMATION, result.getResultType());
        assertEquals("Finance", result.getAssetDepartment());
        assertEquals(AuditStatus.NOT_AUDITED, database.assetDao().getByTag(SESSION_ID, "F001").auditStatus);
        assertEquals(0, database.departmentAuditDao().getDepartment(SESSION_ID, "IT").scannedCount);
        assertEquals(1, database.auditEventDao().countBySession(SESSION_ID));
    }

    @Test
    public void unknownAndInvalidScansDoNotMutateProgress() {
        ScanProcessingResult unknown = processor.processScan(request("MISSING"));
        ScanProcessingResult invalid = processor.processScan(request(" "));

        assertFalse(unknown.isSuccess());
        assertEquals(ScanResultType.UNKNOWN_ASSET_REQUIRES_INPUT, unknown.getResultType());
        assertFalse(invalid.isSuccess());
        assertEquals(ScanResultType.INVALID_SCAN, invalid.getResultType());
        assertEquals(0, database.departmentAuditDao().getDepartment(SESSION_ID, "IT").scannedCount);
        assertEquals(1, database.auditEventDao().countBySession(SESSION_ID));
        assertNotNull(database.assetDao().getByTag(SESSION_ID, "0011"));
    }

    private ScanRequest request(String barcode) {
        return new ScanRequest(SESSION_ID, "IT", barcode, "MANUAL");
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
