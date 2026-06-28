package uk.co.hsim.assetaudit.service;

import uk.co.hsim.assetaudit.data.dao.DiagnosticLogDao;
import uk.co.hsim.assetaudit.util.clock.Clock;

public final class DiagnosticRetentionService {
    private static final long THIRTY_DAYS_MS = 30L * 24L * 60L * 60L * 1000L;
    private static final int DEFAULT_KEEP_COUNT = 500;

    private final DiagnosticLogDao dao;
    private final Clock clock;

    public DiagnosticRetentionService(DiagnosticLogDao dao, Clock clock) {
        this.dao = dao;
        this.clock = clock;
    }

    public void applyDefaultRetention() {
        dao.deleteOlderThan(clock.nowUtcMillis() - THIRTY_DAYS_MS);
        dao.keepMostRecent(DEFAULT_KEEP_COUNT);
    }
}
