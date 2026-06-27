package uk.co.hsim.assetaudit.service;

import uk.co.hsim.assetaudit.data.db.AuditDatabase;
import uk.co.hsim.assetaudit.util.device.DeviceInfoProvider;

public final class AppStartupService {
    private final SettingsService settingsService;
    private final DiagnosticService diagnosticService;
    private final DeviceInfoProvider deviceInfoProvider;

    public AppStartupService(SettingsService settingsService, DiagnosticService diagnosticService,
                             DeviceInfoProvider deviceInfoProvider) {
        this.settingsService = settingsService;
        this.diagnosticService = diagnosticService;
        this.deviceInfoProvider = deviceInfoProvider;
    }

    public void initialiseApplication() {
        settingsService.seedDefaults();
        diagnosticService.logInfo("Startup", "App " + deviceInfoProvider.getAppVersionName()
                + " opened database " + AuditDatabase.DATABASE_NAME
                + " v" + AuditDatabase.DATABASE_VERSION);
        diagnosticService.logInfo("Device", deviceInfoProvider.getManufacturer()
                + " " + deviceInfoProvider.getModel()
                + " Android " + deviceInfoProvider.getAndroidRelease()
                + " API " + deviceInfoProvider.getSdkInt());
    }
}
