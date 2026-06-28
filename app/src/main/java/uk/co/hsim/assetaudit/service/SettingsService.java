package uk.co.hsim.assetaudit.service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import uk.co.hsim.assetaudit.data.entity.AppSettingEntity;
import uk.co.hsim.assetaudit.repository.SettingsRepository;
import uk.co.hsim.assetaudit.scanner.DataWedgeConstants;
import uk.co.hsim.assetaudit.util.clock.Clock;

public final class SettingsService {
    private final SettingsRepository repository;
    private final Clock clock;

    public SettingsService(SettingsRepository repository, Clock clock) {
        this.repository = repository;
        this.clock = clock;
    }

    public void seedDefaults() {
        for (Map.Entry<String, String> entry : defaultSettings().entrySet()) {
            if (repository.get(entry.getKey()) == null) {
                repository.put(new AppSettingEntity(entry.getKey(), entry.getValue(), clock.nowUtcMillis()));
            }
        }
        if (repository.get(SettingsKeys.DATABASE_CREATED_AT_UTC) == null) {
            repository.put(new AppSettingEntity(
                    SettingsKeys.DATABASE_CREATED_AT_UTC,
                    String.valueOf(clock.nowUtcMillis()),
                    clock.nowUtcMillis()
            ));
        }
    }

    public boolean getBooleanSetting(String key, boolean defaultValue) {
        AppSettingEntity entity = repository.get(key);
        if (entity == null) {
            return defaultValue;
        }
        return Boolean.parseBoolean(entity.settingValue);
    }

    public String getStringSetting(String key, String defaultValue) {
        AppSettingEntity entity = repository.get(key);
        return entity == null ? defaultValue : entity.settingValue;
    }

    public void setBooleanSetting(String key, boolean value) {
        repository.put(new AppSettingEntity(key, String.valueOf(value), clock.nowUtcMillis()));
    }

    public void setStringSetting(String key, String value) {
        repository.put(new AppSettingEntity(key, value, clock.nowUtcMillis()));
    }

    public List<AppSettingEntity> listAll() {
        return repository.listAll();
    }

    public static Map<String, String> defaultSettings() {
        Map<String, String> settings = new LinkedHashMap<>();
        settings.put(SettingsKeys.REQUIRE_MOVEMENT_CONFIRMATION, "true");
        settings.put(SettingsKeys.ALLOW_MANUAL_BARCODE_ENTRY, "true");
        settings.put(SettingsKeys.ALLOW_NEW_ASSET_CREATION, "true");
        settings.put(SettingsKeys.UNASSIGNED_DEPARTMENT_LABEL, "Unassigned / Blank Department");
        settings.put(SettingsKeys.DEFAULT_EXPORT_FORMAT, "CSV");
        settings.put(SettingsKeys.ALLOW_DRAFT_EXPORTS, "false");
        settings.put(SettingsKeys.DIAGNOSTIC_LOGGING_ENABLED, "true");
        settings.put(SettingsKeys.LIVE_SCANNER_ENABLED, "true");
        settings.put(SettingsKeys.DATAWEDGE_PROFILE_NAME, DataWedgeConstants.PROFILE_NAME);
        settings.put(SettingsKeys.DATAWEDGE_INTENT_ACTION, DataWedgeConstants.ACTION_SCAN);
        settings.put(SettingsKeys.DATAWEDGE_INTENT_CATEGORY, DataWedgeConstants.CATEGORY_SCAN);
        settings.put(SettingsKeys.DATAWEDGE_DEBOUNCE_MS, "750");
        settings.put(SettingsKeys.SCANNER_DIAGNOSTICS_ENABLED, "true");
        return settings;
    }
}
