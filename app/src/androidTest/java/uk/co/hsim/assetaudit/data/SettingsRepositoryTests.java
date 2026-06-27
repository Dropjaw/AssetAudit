package uk.co.hsim.assetaudit.data;

import static org.junit.Assert.assertEquals;

import androidx.room.Room;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import uk.co.hsim.assetaudit.data.db.AuditDatabase;
import uk.co.hsim.assetaudit.data.repository.RoomSettingsRepository;
import uk.co.hsim.assetaudit.repository.SettingsRepository;
import uk.co.hsim.assetaudit.service.SettingsKeys;

@RunWith(AndroidJUnit4.class)
public class SettingsRepositoryTests {
    private AuditDatabase database;
    private SettingsRepository repository;

    @Before
    public void setUp() {
        database = Room.inMemoryDatabaseBuilder(
                        InstrumentationRegistry.getInstrumentation().getTargetContext(),
                        AuditDatabase.class
                )
                .allowMainThreadQueries()
                .build();
        repository = new RoomSettingsRepository(database.appSettingDao());
    }

    @After
    public void tearDown() {
        database.close();
    }

    @Test
    public void settingPersistsAndCanBeUpdated() {
        repository.put(TestEntities.setting(SettingsKeys.REQUIRE_MOVEMENT_CONFIRMATION, "true"));
        assertEquals("true", repository.get(SettingsKeys.REQUIRE_MOVEMENT_CONFIRMATION).settingValue);

        repository.put(TestEntities.setting(SettingsKeys.REQUIRE_MOVEMENT_CONFIRMATION, "false"));
        assertEquals("false", repository.get(SettingsKeys.REQUIRE_MOVEMENT_CONFIRMATION).settingValue);
    }
}
