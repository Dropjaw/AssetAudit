package uk.co.hsim.assetaudit.repository;

import java.util.List;

import uk.co.hsim.assetaudit.data.entity.AppSettingEntity;

public interface SettingsRepository {
    AppSettingEntity get(String key);

    void put(AppSettingEntity entity);

    List<AppSettingEntity> listAll();
}
