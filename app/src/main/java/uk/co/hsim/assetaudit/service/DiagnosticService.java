package uk.co.hsim.assetaudit.service;

import java.util.List;

import uk.co.hsim.assetaudit.data.entity.DiagnosticLogEntity;
import uk.co.hsim.assetaudit.repository.DiagnosticRepository;
import uk.co.hsim.assetaudit.util.clock.Clock;
import uk.co.hsim.assetaudit.util.logging.AppLogger;

public final class DiagnosticService {
    private final DiagnosticRepository repository;
    private final Clock clock;
    private final AppLogger logger;

    public DiagnosticService(DiagnosticRepository repository, Clock clock, AppLogger logger) {
        this.repository = repository;
        this.clock = clock;
        this.logger = logger;
    }

    public void logInfo(String component, String message) {
        logger.info(component, message);
        repository.insert(new DiagnosticLogEntity(clock.nowUtcMillis(), "INFO", component, message, null));
    }

    public void logWarning(String component, String message) {
        logger.warning(component, message);
        repository.insert(new DiagnosticLogEntity(clock.nowUtcMillis(), "WARNING", component, message, null));
    }

    public void logError(String component, String message, Throwable throwable) {
        logger.error(component, message, throwable);
        String summary = throwable == null ? null : throwable.getClass().getSimpleName() + ": " + throwable.getMessage();
        repository.insert(new DiagnosticLogEntity(clock.nowUtcMillis(), "ERROR", component, message, summary));
    }

    public List<DiagnosticLogEntity> listRecent(int limit) {
        return repository.listRecent(limit);
    }
}
