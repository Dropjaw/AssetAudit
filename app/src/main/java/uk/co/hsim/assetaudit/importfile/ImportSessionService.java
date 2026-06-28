package uk.co.hsim.assetaudit.importfile;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import uk.co.hsim.assetaudit.data.db.AuditDatabase;
import uk.co.hsim.assetaudit.data.entity.AssetEntity;
import uk.co.hsim.assetaudit.data.entity.AuditEventEntity;
import uk.co.hsim.assetaudit.data.entity.AuditSessionEntity;
import uk.co.hsim.assetaudit.data.entity.DepartmentAuditEntity;
import uk.co.hsim.assetaudit.data.entity.ImportIssueEntity;
import uk.co.hsim.assetaudit.data.entity.LookupValueEntity;
import uk.co.hsim.assetaudit.domain.enums.AuditStatus;
import uk.co.hsim.assetaudit.domain.enums.DepartmentAuditStatus;
import uk.co.hsim.assetaudit.domain.enums.EventKind;
import uk.co.hsim.assetaudit.domain.enums.ImportIssueSeverity;
import uk.co.hsim.assetaudit.domain.enums.SessionStatus;
import uk.co.hsim.assetaudit.domain.results.ErrorCode;
import uk.co.hsim.assetaudit.domain.results.OperationResult;
import uk.co.hsim.assetaudit.hardening.DiagnosticRedactor;
import uk.co.hsim.assetaudit.service.SettingsKeys;
import uk.co.hsim.assetaudit.service.SettingsService;
import uk.co.hsim.assetaudit.util.clock.Clock;
import uk.co.hsim.assetaudit.util.device.DeviceInfoProvider;
import uk.co.hsim.assetaudit.util.identity.UserIdentityProvider;

public final class ImportSessionService {
    private static final long MAX_IMPORT_SIZE_BYTES = 20L * 1024L * 1024L;
    private static final int MAX_IMPORT_ROWS = 20000;

    private final AuditDatabase database;
    private final SettingsService settingsService;
    private final Clock clock;
    private final UserIdentityProvider userIdentityProvider;
    private final DeviceInfoProvider deviceInfoProvider;
    private final DiagnosticRedactor diagnosticRedactor;
    private final List<AssetFileReader> readers;

    public ImportSessionService(AuditDatabase database, SettingsService settingsService, Clock clock,
                                UserIdentityProvider userIdentityProvider,
                                DeviceInfoProvider deviceInfoProvider) {
        this.database = database;
        this.settingsService = settingsService;
        this.clock = clock;
        this.userIdentityProvider = userIdentityProvider;
        this.deviceInfoProvider = deviceInfoProvider;
        this.diagnosticRedactor = new DiagnosticRedactor();
        this.readers = new ArrayList<>();
        readers.add(new CsvAssetFileReader());
        readers.add(new XlsxAssetFileReader());
    }

    public OperationResult<ImportPreview> previewImport(DocumentSource source) {
        DocumentReference reference = source.getReference();
        if (reference.getDetectedFormat() == AssetFileFormat.UNSUPPORTED) {
            return OperationResult.fail(ErrorCode.VALIDATION_FAILED, "Unsupported file type.");
        }
        if (reference.getSizeBytes() > MAX_IMPORT_SIZE_BYTES) {
            return OperationResult.fail(ErrorCode.VALIDATION_FAILED, "Asset file is too large for pilot import.");
        }
        AssetFileReader reader = readerFor(reference.getDetectedFormat());
        if (reader == null) {
            return OperationResult.fail(ErrorCode.VALIDATION_FAILED, "Unsupported file type.");
        }
        try {
            ParsedAssetFile parsedFile = reader.read(source);
            if (parsedFile.getMetadata().getSourceRowCount() > MAX_IMPORT_ROWS) {
                return OperationResult.fail(ErrorCode.VALIDATION_FAILED, "Asset file has too many rows for pilot import.");
            }
            AssetImportMapper mapper = new AssetImportMapper();
            HeaderDetectionResult headerDetection = mapper.detectHeaders(parsedFile);
            List<AssetImportRow> rows = mapper.mapRows(parsedFile);
            ImportSettings settings = ImportSettings.defaults(settingsService.getStringSetting(
                    SettingsKeys.UNASSIGNED_DEPARTMENT_LABEL, "Unassigned / Blank Department"));
            ImportValidationResult validation = new AssetImportValidator()
                    .validate(parsedFile, rows, headerDetection, settings);
            ImportPreview preview = new ImportPreviewBuilder()
                    .build(reference, parsedFile, headerDetection, validation, settings);
            return OperationResult.ok(preview);
        } catch (AssetFileReadException e) {
            return OperationResult.fail(ErrorCode.VALIDATION_FAILED, e.getMessage());
        }
    }

    public OperationResult<CreatedAuditSession> createSessionFromPreview(ImportPreview preview,
                                                                         ImportConfirmation confirmation) {
        if (!preview.getValidationResult().isImportable()) {
            return OperationResult.fail(ErrorCode.VALIDATION_FAILED, "Fatal import issues must be resolved before session creation.");
        }
        if (preview.getValidationResult().getWarningCount() > 0 && !confirmation.isWarningsAccepted()) {
            return OperationResult.fail(ErrorCode.VALIDATION_FAILED, "Warnings must be confirmed before session creation.");
        }
        AtomicReference<CreatedAuditSession> created = new AtomicReference<>();
        try {
            database.runInTransaction(() -> created.set(createSessionRows(preview)));
            return OperationResult.ok(created.get());
        } catch (RuntimeException e) {
            return OperationResult.fail(ErrorCode.DATABASE_ERROR, "Import transaction failed: " + e.getMessage());
        }
    }

    private CreatedAuditSession createSessionRows(ImportPreview preview) {
        long now = clock.nowUtcMillis();
        String sessionId = UUID.randomUUID().toString();
        String auditName = preview.getParsedFile().getMetadata().getAuditName();
        if (auditName.isEmpty()) {
            auditName = preview.getDocumentReference().getDisplayName();
        }
        AuditSessionEntity session = new AuditSessionEntity(
                sessionId,
                auditName,
                diagnosticRedactor.safeDisplayName(preview.getDocumentReference().getDisplayName()),
                preview.getDocumentReference().getUri() == null
                        ? null
                        : diagnosticRedactor.redactUri(preview.getDocumentReference().getUri().toString()),
                now,
                null,
                SessionStatus.ACTIVE,
                userIdentityProvider.getDisplayName(),
                deviceInfoProvider.getModel(),
                AuditDatabase.DATABASE_VERSION
        );
        session.sourceFormat = preview.getDocumentReference().getDetectedFormat().name();
        session.sourceRowCount = preview.getParsedFile().getMetadata().getSourceRowCount();
        session.importedAtUtc = now;
        database.auditSessionDao().insert(session);

        ImportSettings settings = ImportSettings.defaults(settingsService.getStringSetting(
                SettingsKeys.UNASSIGNED_DEPARTMENT_LABEL, "Unassigned / Blank Department"));
        List<AssetEntity> assets = toAssetEntities(sessionId, preview.getValidationResult().getAcceptedRows(), settings, now);
        database.assetDao().insertAll(assets);
        database.importIssueDao().insertAll(toIssueEntities(sessionId, preview.getValidationResult().getIssues()));
        insertDepartments(sessionId, preview.getDepartmentCounts());
        database.lookupValueDao().insertAll(toLookupValues(preview.getValidationResult().getAcceptedRows(), settings));
        database.auditEventDao().insert(new AuditEventEntity(
                UUID.randomUUID().toString(),
                sessionId,
                null,
                EventKind.SESSION_CREATED,
                null,
                null,
                null,
                now,
                userIdentityProvider.getDisplayName(),
                deviceInfoProvider.getModel(),
                "Imported " + assets.size() + " assets from " + preview.getDocumentReference().getDisplayName()
        ));
        return new CreatedAuditSession(sessionId, assets.size(), preview.getDepartmentCounts().size());
    }

    private List<AssetEntity> toAssetEntities(String sessionId, List<AssetImportRow> rows,
                                              ImportSettings settings, long now) {
        List<AssetEntity> entities = new ArrayList<>();
        for (AssetImportRow row : rows) {
            AssetEntity entity = new AssetEntity(
                    sessionId,
                    row.getAssetTagId(),
                    ImportPreviewBuilder.normalizeDepartment(row.getDepartment(), settings.getUnassignedDepartmentLabel()),
                    null,
                    row.getDescription(),
                    row.getStatus(),
                    row.getSite(),
                    row.getLocation(),
                    row.getCategory(),
                    row.getSubCategory(),
                    row.getOwner(),
                    row.getPrimaryUser(),
                    AuditStatus.NOT_AUDITED,
                    false,
                    now
            );
            entity.brand = row.getBrand();
            entity.model = row.getModel();
            entity.auditDetails = row.getAuditDetails();
            entity.sourceRowNumber = row.getSourceRowNumber();
            entity.importedAtUtc = now;
            entities.add(entity);
        }
        return entities;
    }

    private List<ImportIssueEntity> toIssueEntities(String sessionId, List<ImportIssue> issues) {
        List<ImportIssueEntity> entities = new ArrayList<>();
        for (ImportIssue issue : issues) {
            if (issue.getSeverity() == ImportIssueSeverity.FATAL) {
                continue;
            }
            entities.add(new ImportIssueEntity(
                    sessionId,
                    issue.getRowNumber(),
                    issue.getColumnName(),
                    issue.getSeverity(),
                    issue.getCode().name(),
                    issue.getMessage(),
                    issue.getSourceValue(),
                    false
            ));
        }
        return entities;
    }

    private void insertDepartments(String sessionId, Map<String, Integer> counts) {
        for (Map.Entry<String, Integer> entry : counts.entrySet()) {
            database.departmentAuditDao().insert(new DepartmentAuditEntity(
                    sessionId,
                    entry.getKey(),
                    entry.getValue(),
                    0,
                    0,
                    0,
                    0,
                    DepartmentAuditStatus.NOT_STARTED,
                    null
            ));
        }
    }

    private List<LookupValueEntity> toLookupValues(List<AssetImportRow> rows, ImportSettings settings) {
        Map<String, LookupValueEntity> lookups = new LinkedHashMap<>();
        int order = 0;
        for (AssetImportRow row : rows) {
            order = putLookup(lookups, "department", ImportPreviewBuilder.normalizeDepartment(
                    row.getDepartment(), settings.getUnassignedDepartmentLabel()), order);
            order = putLookup(lookups, "status", row.getStatus(), order);
            order = putLookup(lookups, "site", row.getSite(), order);
            order = putLookup(lookups, "location", row.getLocation(), order);
            order = putLookup(lookups, "category", row.getCategory(), order);
            order = putLookup(lookups, "sub_category", row.getSubCategory(), order);
            order = putLookup(lookups, "owner", row.getOwner(), order);
        }
        return new ArrayList<>(lookups.values());
    }

    private int putLookup(Map<String, LookupValueEntity> lookups, String type, String value, int order) {
        if (value == null || value.trim().isEmpty()) {
            return order;
        }
        String key = type + "\u0000" + value.trim();
        if (!lookups.containsKey(key)) {
            lookups.put(key, new LookupValueEntity(type, value.trim(), order, true));
            return order + 1;
        }
        return order;
    }

    private AssetFileReader readerFor(AssetFileFormat format) {
        for (AssetFileReader reader : readers) {
            if (reader.canRead(format)) {
                return reader;
            }
        }
        return null;
    }
}
