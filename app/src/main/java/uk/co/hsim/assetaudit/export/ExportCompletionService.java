package uk.co.hsim.assetaudit.export;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import uk.co.hsim.assetaudit.data.db.AuditDatabase;
import uk.co.hsim.assetaudit.data.entity.AuditEventEntity;
import uk.co.hsim.assetaudit.data.entity.ExportFileEntity;
import uk.co.hsim.assetaudit.data.entity.ExportRunEntity;
import uk.co.hsim.assetaudit.domain.enums.EventKind;
import uk.co.hsim.assetaudit.domain.results.OperationResult;
import uk.co.hsim.assetaudit.util.device.DeviceInfoProvider;
import uk.co.hsim.assetaudit.util.identity.UserIdentityProvider;

public final class ExportCompletionService {
    private final AuditDatabase database;
    private final DeviceInfoProvider deviceInfoProvider;
    private final UserIdentityProvider userIdentityProvider;

    public ExportCompletionService(AuditDatabase database, DeviceInfoProvider deviceInfoProvider,
                                   UserIdentityProvider userIdentityProvider) {
        this.database = database;
        this.deviceInfoProvider = deviceInfoProvider;
        this.userIdentityProvider = userIdentityProvider;
    }

    public OperationResult<String> recordSuccess(ExportSnapshot snapshot, ExportOptions options,
                                                 ExportPackageResult packageResult, String destinationName) {
        database.runInTransaction(() -> {
            String runId = UUID.randomUUID().toString();
            database.exportRunDao().insertRun(new ExportRunEntity(
                    runId,
                    snapshot.session.sessionId,
                    packageResult.getPackageId(),
                    destinationName,
                    options.getExportMode().name(),
                    snapshot.preview.getReadiness().name(),
                    snapshot.exportedAtUtc,
                    deviceInfoProvider.getAppVersionName(),
                    deviceInfoProvider.getModel(),
                    userIdentityProvider.getDisplayName(),
                    packageResult.getManifestJson()
            ));
            List<ExportFileEntity> files = new ArrayList<>();
            for (ExportFileRecord file : packageResult.getFiles()) {
                files.add(new ExportFileEntity(runId, file.getFileName(), file.getMediaType(), file.getRowCount(), file.getSha256()));
            }
            database.exportRunDao().insertFiles(files);
            database.auditEventDao().insert(new AuditEventEntity(
                    UUID.randomUUID().toString(),
                    snapshot.session.sessionId,
                    null,
                    EventKind.SESSION_EXPORTED,
                    null,
                    null,
                    null,
                    snapshot.exportedAtUtc,
                    userIdentityProvider.getDisplayName(),
                    deviceInfoProvider.getModel(),
                    "Export package created: " + destinationName
            ));
        });
        return OperationResult.ok("Export package created.");
    }
}
