package uk.co.hsim.assetaudit.scanner;

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

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import uk.co.hsim.assetaudit.app.AppExecutors;
import uk.co.hsim.assetaudit.data.db.AuditDatabase;
import uk.co.hsim.assetaudit.data.entity.AssetEntity;
import uk.co.hsim.assetaudit.data.entity.AuditSessionEntity;
import uk.co.hsim.assetaudit.data.entity.DepartmentAuditEntity;
import uk.co.hsim.assetaudit.data.repository.RoomAssetRepository;
import uk.co.hsim.assetaudit.data.repository.RoomDepartmentAuditRepository;
import uk.co.hsim.assetaudit.data.repository.RoomDiagnosticRepository;
import uk.co.hsim.assetaudit.domain.enums.AuditStatus;
import uk.co.hsim.assetaudit.domain.enums.DepartmentAuditStatus;
import uk.co.hsim.assetaudit.domain.enums.SessionStatus;
import uk.co.hsim.assetaudit.service.DepartmentSummaryService;
import uk.co.hsim.assetaudit.service.DiagnosticService;
import uk.co.hsim.assetaudit.service.ScanProcessor;
import uk.co.hsim.assetaudit.util.device.DeviceInfoProvider;
import uk.co.hsim.assetaudit.util.logging.AndroidAppLogger;

@RunWith(AndroidJUnit4.class)
public class ScannerEventRouterTests {
    private static final String SESSION_ID = "scanner-router";
    private AuditDatabase database;
    private ScannerEventRouter router;
    private MutableContextProvider contextProvider;
    private long now;

    @Before
    public void setUp() {
        Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        database = Room.inMemoryDatabaseBuilder(context, AuditDatabase.class)
                .allowMainThreadQueries()
                .build();
        now = 1000L;
        AppExecutors executors = new AppExecutors();
        DiagnosticService diagnostics = new DiagnosticService(
                new RoomDiagnosticRepository(database.diagnosticLogDao()),
                () -> now,
                new AndroidAppLogger()
        );
        DepartmentSummaryService summaries = new DepartmentSummaryService(
                new RoomDepartmentAuditRepository(database.departmentAuditDao()),
                new RoomAssetRepository(database.assetDao())
        );
        ScanProcessor processor = new ScanProcessor(database, () -> now, () -> "tester", new FakeDeviceInfoProvider());
        router = new ScannerEventRouter(executors, processor, summaries, diagnostics, () -> now, 750L);

        AuditSessionEntity session = new AuditSessionEntity(
                SESSION_ID,
                "Router audit",
                null,
                null,
                1000L,
                null,
                SessionStatus.ACTIVE,
                "tester",
                "device",
                1
        );
        database.auditSessionDao().insert(session);
        database.departmentAuditDao().insert(new DepartmentAuditEntity(
                SESSION_ID,
                "IT",
                1,
                0,
                0,
                0,
                0,
                DepartmentAuditStatus.NOT_STARTED,
                null
        ));
        database.assetDao().insert(new AssetEntity(
                SESSION_ID,
                "0011",
                "IT",
                null,
                "Monitor",
                "Available",
                "Main",
                "Unit 11",
                "Computer equipment",
                "Monitor",
                null,
                null,
                AuditStatus.NOT_AUDITED,
                false,
                2000L
        ));
        contextProvider = new MutableContextProvider(session, "IT", true, true);
    }

    @After
    public void tearDown() {
        database.close();
    }

    @Test
    public void expectedDataWedgeScanRoutesToScanProcessor() throws Exception {
        ScannerRouteResult result = route(payload("0011"));

        assertTrue(result.isProcessed());
        assertTrue(result.getScanResult().isSuccess());
        assertEquals(AuditStatus.AUDITED_EXPECTED, database.assetDao().getByTag(SESSION_ID, "0011").auditStatus);
        assertEquals(1, database.departmentAuditDao().getDepartment(SESSION_ID, "IT").scannedCount);
    }

    @Test
    public void ignoresNoSessionNoDepartmentDisabledInactiveAndTransportDuplicate() throws Exception {
        contextProvider.session = null;
        assertFalse(route(payload("0011")).isProcessed());

        contextProvider.session = database.auditSessionDao().getLatestActive();
        contextProvider.department = "";
        assertFalse(route(payload("0011")).isProcessed());

        contextProvider.department = "IT";
        contextProvider.enabled = false;
        assertFalse(route(payload("0011")).isProcessed());

        contextProvider.enabled = true;
        contextProvider.visible = false;
        assertFalse(route(payload("0011")).isProcessed());

        contextProvider.visible = true;
        assertTrue(route(payload("0011")).isProcessed());
        assertFalse(route(payload("0011")).isProcessed());
        assertEquals(1, database.departmentAuditDao().getDepartment(SESSION_ID, "IT").scannedCount);
    }

    private ScannerRouteResult route(ScannerPayload payload) throws Exception {
        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<ScannerRouteResult> result = new AtomicReference<>();
        router.route(payload, contextProvider, value -> {
            result.set(value);
            latch.countDown();
        });
        assertTrue(latch.await(5, TimeUnit.SECONDS));
        return result.get();
    }

    private ScannerPayload payload(String data) {
        return new ScannerPayload(data, "LABEL-TYPE-CODE128", now, "scanner");
    }

    private static final class MutableContextProvider implements ScannerEventRouter.ContextProvider {
        private AuditSessionEntity session;
        private String department;
        private boolean visible;
        private boolean enabled;

        MutableContextProvider(AuditSessionEntity session, String department, boolean visible, boolean enabled) {
            this.session = session;
            this.department = department;
            this.visible = visible;
            this.enabled = enabled;
        }

        @Override
        public AuditSessionEntity getActiveSession() {
            return session;
        }

        @Override
        public String getSelectedDepartment() {
            return department;
        }

        @Override
        public boolean isAuditScanVisible() {
            return visible;
        }

        @Override
        public boolean isLiveScannerEnabled() {
            return enabled;
        }
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
