package uk.co.hsim.assetaudit.data;

import static org.junit.Assert.assertEquals;

import androidx.room.Room;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.List;

import uk.co.hsim.assetaudit.data.db.AuditDatabase;
import uk.co.hsim.assetaudit.data.entity.AuditEventEntity;

@RunWith(AndroidJUnit4.class)
public class AuditEventDaoTests {
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
    }

    @After
    public void tearDown() {
        database.close();
    }

    @Test
    public void eventsListInTimestampOrder() {
        database.auditEventDao().insert(TestEntities.event("event-2", "session-1", "0011", 2000L));
        database.auditEventDao().insert(TestEntities.event("event-1", "session-1", "0011", 1000L));

        List<AuditEventEntity> events = database.auditEventDao().listBySession("session-1");

        assertEquals("event-1", events.get(0).eventId);
        assertEquals("event-2", events.get(1).eventId);
        assertEquals(2, database.auditEventDao().listByAssetTag("session-1", "0011").size());
    }
}
