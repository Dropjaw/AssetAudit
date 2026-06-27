package uk.co.hsim.assetaudit.data;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThrows;

import android.database.sqlite.SQLiteConstraintException;

import androidx.room.Room;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import uk.co.hsim.assetaudit.data.db.AuditDatabase;
import uk.co.hsim.assetaudit.data.entity.AssetEntity;

@RunWith(AndroidJUnit4.class)
public class AssetDaoTests {
    private AuditDatabase database;

    @Before
    public void setUp() {
        database = Room.inMemoryDatabaseBuilder(
                        InstrumentationRegistry.getInstrumentation().getTargetContext(),
                        AuditDatabase.class
                )
                .allowMainThreadQueries()
                .build();
        database.auditSessionDao().insert(TestEntities.session("session-1"));
        database.auditSessionDao().insert(TestEntities.session("session-2"));
    }

    @After
    public void tearDown() {
        database.close();
    }

    @Test
    public void assetTagIsStoredAsExactText() {
        database.assetDao().insert(TestEntities.asset("session-1", "0011", "IT"));
        AssetEntity stored = database.assetDao().getByTag("session-1", "0011");
        assertNotNull(stored);
        assertEquals("0011", stored.assetTagId);
    }

    @Test
    public void duplicateAssetTagWithinSameSessionIsRejected() {
        database.assetDao().insert(TestEntities.asset("session-1", "0011", "IT"));
        assertThrows(SQLiteConstraintException.class,
                () -> database.assetDao().insert(TestEntities.asset("session-1", "0011", "IT")));
    }

    @Test
    public void sameAssetTagInDifferentSessionsIsAllowed() {
        database.assetDao().insert(TestEntities.asset("session-1", "0011", "IT"));
        database.assetDao().insert(TestEntities.asset("session-2", "0011", "IT"));
        assertEquals(1, database.assetDao().countBySession("session-1"));
        assertEquals(1, database.assetDao().countBySession("session-2"));
    }
}
