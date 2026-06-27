package uk.co.hsim.assetaudit.data;

import static org.junit.Assert.assertEquals;
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
import uk.co.hsim.assetaudit.data.entity.DepartmentAuditEntity;
import uk.co.hsim.assetaudit.domain.enums.DepartmentAuditStatus;

@RunWith(AndroidJUnit4.class)
public class DepartmentAuditDaoTests {
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
    public void departmentIsUniqueWithinSession() {
        database.departmentAuditDao().insert(TestEntities.department("session-1", "IT"));
        assertThrows(SQLiteConstraintException.class,
                () -> database.departmentAuditDao().insert(TestEntities.department("session-1", "IT")));
    }

    @Test
    public void statusUpdateWorks() {
        database.departmentAuditDao().insert(TestEntities.department("session-1", "IT"));
        database.departmentAuditDao().updateStatus("session-1", "IT", DepartmentAuditStatus.IN_PROGRESS, null);
        DepartmentAuditEntity stored = database.departmentAuditDao().getDepartment("session-1", "IT");
        assertEquals(DepartmentAuditStatus.IN_PROGRESS, stored.status);
    }
}
