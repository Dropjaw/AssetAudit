package uk.co.hsim.assetaudit.data;

import static org.junit.Assert.assertEquals;
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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import uk.co.hsim.assetaudit.data.db.AuditDatabase;
import uk.co.hsim.assetaudit.data.entity.AssetEntity;
import uk.co.hsim.assetaudit.domain.results.OperationResult;
import uk.co.hsim.assetaudit.importfile.AssetFileFormat;
import uk.co.hsim.assetaudit.importfile.CreatedAuditSession;
import uk.co.hsim.assetaudit.importfile.DocumentReference;
import uk.co.hsim.assetaudit.importfile.DocumentSource;
import uk.co.hsim.assetaudit.importfile.ImportConfirmation;
import uk.co.hsim.assetaudit.importfile.ImportPreview;
import uk.co.hsim.assetaudit.importfile.ImportSessionService;
import uk.co.hsim.assetaudit.data.repository.RoomSettingsRepository;
import uk.co.hsim.assetaudit.service.SettingsService;
import uk.co.hsim.assetaudit.util.clock.Clock;
import uk.co.hsim.assetaudit.util.device.DeviceInfoProvider;
import uk.co.hsim.assetaudit.util.identity.UserIdentityProvider;

@RunWith(AndroidJUnit4.class)
public class ImportSessionServiceTests {
    private AuditDatabase database;
    private ImportSessionService service;

    @Before
    public void setUp() {
        Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        database = Room.inMemoryDatabaseBuilder(context, AuditDatabase.class)
                .allowMainThreadQueries()
                .build();
        SettingsService settingsService = new SettingsService(new RoomSettingsRepository(database.appSettingDao()), () -> 1000L);
        settingsService.seedDefaults();
        service = new ImportSessionService(database, settingsService, () -> 2000L,
                () -> "tester", new FakeDeviceInfoProvider());
    }

    @After
    public void tearDown() {
        database.close();
    }

    @Test
    public void suppliedWorkbookCreatesCompleteSession() throws Exception {
        OperationResult<ImportPreview> previewResult = service.previewImport(assetFixtureSource());
        assertTrue(previewResult.isSuccess());

        OperationResult<CreatedAuditSession> createResult = service.createSessionFromPreview(
                previewResult.getValue(), new ImportConfirmation(true));

        assertTrue(createResult.isSuccess());
        CreatedAuditSession created = createResult.getValue();
        assertEquals(569, created.getAssetCount());
        assertEquals(18, created.getDepartmentCount());
        assertEquals(569, database.assetDao().countBySession(created.getSessionId()));
        assertEquals(18, database.departmentAuditDao().listBySession(created.getSessionId()).size());
        assertTrue(database.importIssueDao().listBySession(created.getSessionId()).size() > 0);
        assertTrue(database.lookupValueDao().listActiveByType("department").size() >= 18);

        AssetEntity leadingZero = database.assetDao().getByTag(created.getSessionId(), "0011");
        AssetEntity secondLeadingZero = database.assetDao().getByTag(created.getSessionId(), "0069");
        AssetEntity alphanumeric = database.assetDao().getByTag(created.getSessionId(), "NN93N061612303C1F");
        assertNotNull(leadingZero);
        assertNotNull(secondLeadingZero);
        assertNotNull(alphanumeric);
        assertEquals("0011", leadingZero.assetTagId);
        assertEquals("0069", secondLeadingZero.assetTagId);
        assertEquals("NN93N061612303C1F", alphanumeric.assetTagId);
    }

    @Test
    public void duplicateTagPreviewDoesNotCreatePartialSession() {
        String csv = "Asset Tag ID,Department,Description,Status,Site,Location,Category\n"
                + "0011,IT,Switch,Available,Poole,Unit 11,Computer equipment\n"
                + "0011,IT,Switch,Available,Poole,Unit 11,Computer equipment\n";
        OperationResult<ImportPreview> previewResult = service.previewImport(
                new BytesDocumentSource("duplicate.csv", AssetFileFormat.CSV, csv.getBytes(StandardCharsets.UTF_8)));
        assertTrue(previewResult.isSuccess());

        OperationResult<CreatedAuditSession> createResult = service.createSessionFromPreview(
                previewResult.getValue(), new ImportConfirmation(true));

        assertTrue(!createResult.isSuccess());
        assertEquals(0, database.auditSessionDao().listSessions().size());
    }

    @Test
    public void blankDepartmentImportsUnderConfiguredBucket() {
        String csv = "Asset Tag ID,Department,Description,Status,Site,Location,Category\n"
                + "0011,,Switch,Available,Poole,Unit 11,Computer equipment\n";
        OperationResult<ImportPreview> previewResult = service.previewImport(
                new BytesDocumentSource("blank.csv", AssetFileFormat.CSV, csv.getBytes(StandardCharsets.UTF_8)));
        assertTrue(previewResult.isSuccess());

        OperationResult<CreatedAuditSession> createResult = service.createSessionFromPreview(
                previewResult.getValue(), new ImportConfirmation(true));

        assertTrue(createResult.isSuccess());
        assertNotNull(database.departmentAuditDao().getDepartment(
                createResult.getValue().getSessionId(), "Unassigned / Blank Department"));
    }

    private DocumentSource assetFixtureSource() throws IOException {
        Context context = InstrumentationRegistry.getInstrumentation().getContext();
        try (InputStream inputStream = context.getAssets().open("fixtures/NonAuditedAssets.xlsx")) {
            return new BytesDocumentSource("NonAuditedAssets.xlsx", AssetFileFormat.XLSX, readAll(inputStream));
        }
    }

    private byte[] readAll(InputStream inputStream) throws IOException {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        byte[] buffer = new byte[8192];
        int read;
        while ((read = inputStream.read(buffer)) != -1) {
            outputStream.write(buffer, 0, read);
        }
        return outputStream.toByteArray();
    }

    private static final class BytesDocumentSource implements DocumentSource {
        private final DocumentReference reference;
        private final byte[] bytes;

        BytesDocumentSource(String displayName, AssetFileFormat format, byte[] bytes) {
            this.reference = new DocumentReference(null, displayName, "", bytes.length, format);
            this.bytes = bytes;
        }

        @Override
        public DocumentReference getReference() {
            return reference;
        }

        @Override
        public InputStream openStream() {
            return new ByteArrayInputStream(bytes);
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
