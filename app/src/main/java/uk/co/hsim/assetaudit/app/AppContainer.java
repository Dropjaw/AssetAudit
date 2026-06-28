package uk.co.hsim.assetaudit.app;

import android.content.Context;

import uk.co.hsim.assetaudit.data.db.AuditDatabase;
import uk.co.hsim.assetaudit.data.repository.RoomAssetRepository;
import uk.co.hsim.assetaudit.data.repository.RoomAuditSessionRepository;
import uk.co.hsim.assetaudit.data.repository.RoomDepartmentAuditRepository;
import uk.co.hsim.assetaudit.data.repository.RoomDiagnosticRepository;
import uk.co.hsim.assetaudit.data.repository.RoomSettingsRepository;
import uk.co.hsim.assetaudit.export.CsvReportWriter;
import uk.co.hsim.assetaudit.export.ExportCompletionService;
import uk.co.hsim.assetaudit.export.ExportPackageService;
import uk.co.hsim.assetaudit.export.ExportSnapshotBuilder;
import uk.co.hsim.assetaudit.export.JsonManifestWriter;
import uk.co.hsim.assetaudit.export.ReportPreviewService;
import uk.co.hsim.assetaudit.importfile.ImportSessionService;
import uk.co.hsim.assetaudit.scanner.DataWedgeProfileManager;
import uk.co.hsim.assetaudit.scanner.ScannerEventRouter;
import uk.co.hsim.assetaudit.scanner.ScannerPayloadParser;
import uk.co.hsim.assetaudit.service.AppStartupService;
import uk.co.hsim.assetaudit.service.AuditSessionService;
import uk.co.hsim.assetaudit.service.DepartmentSummaryService;
import uk.co.hsim.assetaudit.service.DiagnosticService;
import uk.co.hsim.assetaudit.service.ExceptionResolutionService;
import uk.co.hsim.assetaudit.service.ScanProcessor;
import uk.co.hsim.assetaudit.service.SettingsService;
import uk.co.hsim.assetaudit.util.clock.Clock;
import uk.co.hsim.assetaudit.util.clock.SystemClock;
import uk.co.hsim.assetaudit.util.device.AndroidDeviceInfoProvider;
import uk.co.hsim.assetaudit.util.device.DeviceInfoProvider;
import uk.co.hsim.assetaudit.util.identity.LocalUserIdentityProvider;
import uk.co.hsim.assetaudit.util.logging.AndroidAppLogger;

public final class AppContainer {
    private static volatile AppContainer instance;

    public final AppExecutors executors;
    public final AuditDatabase database;
    public final SettingsService settingsService;
    public final DiagnosticService diagnosticService;
    public final AuditSessionService auditSessionService;
    public final DepartmentSummaryService departmentSummaryService;
    public final ImportSessionService importSessionService;
    public final ScanProcessor scanProcessor;
    public final ExceptionResolutionService exceptionResolutionService;
    public final ScannerPayloadParser scannerPayloadParser;
    public final ScannerEventRouter scannerEventRouter;
    public final DataWedgeProfileManager dataWedgeProfileManager;
    public final ReportPreviewService reportPreviewService;
    public final ExportSnapshotBuilder exportSnapshotBuilder;
    public final ExportPackageService exportPackageService;
    public final ExportCompletionService exportCompletionService;
    public final AppStartupService appStartupService;
    public final DeviceInfoProvider deviceInfoProvider;
    public final Clock clock;

    private AppContainer(Context context) {
        Context appContext = context.getApplicationContext();
        clock = new SystemClock();
        executors = new AppExecutors();
        database = AuditDatabase.create(appContext);
        deviceInfoProvider = new AndroidDeviceInfoProvider(appContext);

        settingsService = new SettingsService(new RoomSettingsRepository(database.appSettingDao()), clock);
        diagnosticService = new DiagnosticService(
                new RoomDiagnosticRepository(database.diagnosticLogDao()),
                clock,
                new AndroidAppLogger()
        );
        auditSessionService = new AuditSessionService(new RoomAuditSessionRepository(database.auditSessionDao()));
        departmentSummaryService = new DepartmentSummaryService(
                new RoomDepartmentAuditRepository(database.departmentAuditDao()),
                new RoomAssetRepository(database.assetDao())
        );
        importSessionService = new ImportSessionService(
                database,
                settingsService,
                clock,
                new LocalUserIdentityProvider(),
                deviceInfoProvider
        );
        scanProcessor = new ScanProcessor(
                database,
                clock,
                new LocalUserIdentityProvider(),
                deviceInfoProvider
        );
        exceptionResolutionService = new ExceptionResolutionService(
                database,
                departmentSummaryService,
                diagnosticService,
                clock,
                new LocalUserIdentityProvider(),
                deviceInfoProvider
        );
        scannerPayloadParser = new ScannerPayloadParser();
        scannerEventRouter = new ScannerEventRouter(
                executors,
                scanProcessor,
                departmentSummaryService,
                diagnosticService,
                clock,
                750L
        );
        dataWedgeProfileManager = new DataWedgeProfileManager(appContext);
        reportPreviewService = new ReportPreviewService(database, clock);
        exportSnapshotBuilder = new ExportSnapshotBuilder(database, reportPreviewService, clock);
        exportPackageService = new ExportPackageService(
                new CsvReportWriter(),
                new JsonManifestWriter(),
                deviceInfoProvider
        );
        exportCompletionService = new ExportCompletionService(
                database,
                deviceInfoProvider,
                new LocalUserIdentityProvider()
        );
        appStartupService = new AppStartupService(settingsService, diagnosticService, deviceInfoProvider);
    }

    public static AppContainer get(Context context) {
        if (instance == null) {
            synchronized (AppContainer.class) {
                if (instance == null) {
                    instance = new AppContainer(context);
                }
            }
        }
        return instance;
    }
}
