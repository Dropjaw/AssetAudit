package uk.co.hsim.assetaudit.data.repository;

import java.util.List;

import uk.co.hsim.assetaudit.data.dao.AppSettingDao;
import uk.co.hsim.assetaudit.data.entity.AppSettingEntity;
import uk.co.hsim.assetaudit.repository.SettingsRepository;

public final class RoomSettingsRepository implements SettingsRepository {
    private final AppSettingDao dao;

    public RoomSettingsRepository(AppSettingDao dao) {
        this.dao = dao;
    }

    @Override
    public AppSettingEntity get(String key) {
        return dao.get(key);
    }

    @Override
    public void put(AppSettingEntity entity) {
        dao.put(entity);
    }

    @Override
    public List<AppSettingEntity> listAll() {
        return dao.listAll();
    }
}
