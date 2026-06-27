package uk.co.hsim.assetaudit.data;

import static org.junit.Assert.assertNotNull;

import androidx.room.Room;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import uk.co.hsim.assetaudit.data.db.AuditDatabase;

@RunWith(AndroidJUnit4.class)
public class AuditDatabaseCreationTest {
    private AuditDatabase database;

    @Before
    public void setUp() {
        database = Room.inMemoryDatabaseBuilder(
                        InstrumentationRegistry.getInstrumentation().getTargetContext(),
                        AuditDatabase.class
                )
                .allowMainThreadQueries()
                .build();
    }

    @After
    public void tearDown() {
        database.close();
    }

    @Test
    public void databaseOpensAndExposesAllDaos() {
        assertNotNull(database.auditSessionDao());
        assertNotNull(database.assetDao());
        assertNotNull(database.auditEventDao());
        assertNotNull(database.departmentAuditDao());
        assertNotNull(database.importIssueDao());
        assertNotNull(database.lookupValueDao());
        assertNotNull(database.appSettingDao());
        assertNotNull(database.diagnosticLogDao());
    }
}
