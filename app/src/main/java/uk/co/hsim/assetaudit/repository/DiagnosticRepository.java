package uk.co.hsim.assetaudit.repository;

import java.util.List;

import uk.co.hsim.assetaudit.data.entity.DiagnosticLogEntity;

public interface DiagnosticRepository {
    long insert(DiagnosticLogEntity entity);

    List<DiagnosticLogEntity> listRecent(int limit);
}
