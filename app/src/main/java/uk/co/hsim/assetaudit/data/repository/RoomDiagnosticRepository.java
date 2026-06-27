package uk.co.hsim.assetaudit.data.repository;

import java.util.List;

import uk.co.hsim.assetaudit.data.dao.DiagnosticLogDao;
import uk.co.hsim.assetaudit.data.entity.DiagnosticLogEntity;
import uk.co.hsim.assetaudit.repository.DiagnosticRepository;

public final class RoomDiagnosticRepository implements DiagnosticRepository {
    private final DiagnosticLogDao dao;

    public RoomDiagnosticRepository(DiagnosticLogDao dao) {
        this.dao = dao;
    }

    @Override
    public long insert(DiagnosticLogEntity entity) {
        return dao.insert(entity);
    }

    @Override
    public List<DiagnosticLogEntity> listRecent(int limit) {
        return dao.listRecent(limit);
    }
}
