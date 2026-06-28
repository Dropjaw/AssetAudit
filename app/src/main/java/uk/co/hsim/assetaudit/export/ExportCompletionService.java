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
                                                 ExportPackageResult packageResult,
                                                 ExportDestinationSummary destination) {
        OperationResult<ExportCompletionResult> result = recordSuccessWithResult(snapshot, options, packageResult, destination);
        return result.isSuccess()
                ? OperationResult.ok(result.getValue().getMessage())
                : OperationResult.fail(result.getErrorCode(), result.getMessage());
    }

    public OperationResult<ExportCompletionResult> recordSuccessWithResult(ExportSnapshot snapshot, ExportOptions options,
                                                                           ExportPackageResult packageResult,
                                                                           ExportDestinationSummary destination) {
        String destinationName = destination == null ? "" : destination.getDisplayName();
        String destinationSummary = destination == null ? "" : destination.getDiagnosticSummary();
        final String[] runIdRef = new String[1];
        database.runInTransaction(() -> {
            String runId = UUID.randomUUID().toString();
            runIdRef[0] = runId;
            database.exportRunDao().insertRun(new ExportRunEntity(
                    runId,
                    snapshot.session.sessionId,
                    packageResult.getPackageId(),
                    destinationSummary,
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
                    "Export package " + packageResult.getPackageId() + " created: " + destinationSummary
            ));
        });
        return OperationResult.ok(new ExportCompletionResult(runIdRef[0], "Export package created."));
    }
}
