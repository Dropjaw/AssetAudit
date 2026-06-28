package uk.co.hsim.assetaudit.service;

import java.util.List;

import uk.co.hsim.assetaudit.data.entity.DiagnosticLogEntity;
import uk.co.hsim.assetaudit.hardening.DiagnosticRedactor;
import uk.co.hsim.assetaudit.repository.DiagnosticRepository;
import uk.co.hsim.assetaudit.util.clock.Clock;
import uk.co.hsim.assetaudit.util.logging.AppLogger;

public final class DiagnosticService {
    private final DiagnosticRepository repository;
    private final Clock clock;
    private final AppLogger logger;
    private final DiagnosticRedactor redactor;

    public DiagnosticService(DiagnosticRepository repository, Clock clock, AppLogger logger) {
        this(repository, clock, logger, new DiagnosticRedactor());
    }

    public DiagnosticService(DiagnosticRepository repository, Clock clock, AppLogger logger,
                             DiagnosticRedactor redactor) {
        this.repository = repository;
        this.clock = clock;
        this.logger = logger;
        this.redactor = redactor;
    }

    public void logInfo(String component, String message) {
        String safeMessage = redactor.redactMessage(message);
        logger.info(component, safeMessage);
        repository.insert(new DiagnosticLogEntity(clock.nowUtcMillis(), "INFO", component, safeMessage, null));
    }

    public void logWarning(String component, String message) {
        String safeMessage = redactor.redactMessage(message);
        logger.warning(component, safeMessage);
        repository.insert(new DiagnosticLogEntity(clock.nowUtcMillis(), "WARNING", component, safeMessage, null));
    }

    public void logError(String component, String message, Throwable throwable) {
        String safeMessage = redactor.redactMessage(message);
        logger.error(component, safeMessage, throwable);
        String summary = throwable == null ? null : throwable.getClass().getSimpleName() + ": "
                + redactor.redactMessage(throwable.getMessage());
        repository.insert(new DiagnosticLogEntity(clock.nowUtcMillis(), "ERROR", component, safeMessage, summary));
    }

    public List<DiagnosticLogEntity> listRecent(int limit) {
        return repository.listRecent(limit);
    }
}
