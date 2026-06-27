package uk.co.hsim.assetaudit.service;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import uk.co.hsim.assetaudit.data.db.AuditDatabase;
import uk.co.hsim.assetaudit.data.entity.AssetEntity;
import uk.co.hsim.assetaudit.data.entity.AuditEventEntity;
import uk.co.hsim.assetaudit.data.entity.DepartmentAuditEntity;
import uk.co.hsim.assetaudit.domain.enums.AuditStatus;
import uk.co.hsim.assetaudit.domain.enums.DepartmentAuditStatus;
import uk.co.hsim.assetaudit.domain.enums.EventKind;
import uk.co.hsim.assetaudit.domain.enums.ScanResultType;
import uk.co.hsim.assetaudit.domain.rules.BarcodeNormalizer;
import uk.co.hsim.assetaudit.util.clock.Clock;
import uk.co.hsim.assetaudit.util.device.DeviceInfoProvider;
import uk.co.hsim.assetaudit.util.identity.UserIdentityProvider;

public final class ScanProcessor {
    private final AuditDatabase database;
    private final Clock clock;
    private final UserIdentityProvider userIdentityProvider;
    private final DeviceInfoProvider deviceInfoProvider;

    public ScanProcessor(AuditDatabase database, Clock clock, UserIdentityProvider userIdentityProvider,
                         DeviceInfoProvider deviceInfoProvider) {
        this.database = database;
        this.clock = clock;
        this.userIdentityProvider = userIdentityProvider;
        this.deviceInfoProvider = deviceInfoProvider;
    }

    public ScanProcessingResult processScan(ScanRequest request) {
        long now = clock.nowUtcMillis();
        String barcode = BarcodeNormalizer.normalizeAssetTag(request.getBarcodeRaw());
        if (!BarcodeNormalizer.isValidBasicAssetTag(barcode)) {
            return ScanProcessingResult.invalid(request.getSelectedDepartment(), now);
        }
        try {
            AssetEntity asset = database.assetDao().getByTag(request.getSessionId(), barcode);
            if (asset == null) {
                insertRejectedEvent(request, barcode, ScanResultType.UNKNOWN_ASSET_REQUIRES_INPUT,
                        null, null, "Unknown manual scan", now);
                return ScanProcessingResult.unknown(barcode, request.getSelectedDepartment(), now);
            }
            if (asset.auditStatus != AuditStatus.NOT_AUDITED) {
                insertRejectedEvent(request, barcode, ScanResultType.DUPLICATE_SCAN,
                        asset.department, asset.description, "Duplicate manual scan", now);
                return ScanProcessingResult.duplicate(barcode, request.getSelectedDepartment(),
                        asset.department, asset.description, now);
            }
            if (!safeEquals(asset.department, request.getSelectedDepartment())) {
                insertRejectedEvent(request, barcode, ScanResultType.FOUND_IN_OTHER_DEPARTMENT_REQUIRES_CONFIRMATION,
                        asset.department, asset.description, "Found in other department", now);
                return ScanProcessingResult.foundOtherDepartment(barcode, request.getSelectedDepartment(),
                        asset.department, asset.description, now);
            }
            AtomicReference<ScanProcessingResult> result = new AtomicReference<>();
            database.runInTransaction(() -> result.set(acceptExpectedScan(request, asset, now)));
            return result.get();
        } catch (RuntimeException e) {
            return ScanProcessingResult.error(barcode, request.getSelectedDepartment(),
                    "Scan failed. Try again or review diagnostics.", clock.nowUtcMillis());
        }
    }

    private ScanProcessingResult acceptExpectedScan(ScanRequest request, AssetEntity asset, long now) {
        int updated = database.assetDao().updateAuditStatus(
                request.getSessionId(),
                asset.assetTagId,
                AuditStatus.AUDITED_EXPECTED,
                now
        );
        if (updated != 1) {
            throw new IllegalStateException("Expected asset update affected " + updated + " rows.");
        }
        database.auditEventDao().insert(new AuditEventEntity(
                UUID.randomUUID().toString(),
                request.getSessionId(),
                asset.assetTagId,
                EventKind.SCAN_ACCEPTED,
                ScanResultType.SUCCESS_EXPECTED,
                request.getSelectedDepartment(),
                null,
                now,
                userIdentityProvider.getDisplayName(),
                deviceInfoProvider.getModel(),
                "Manual expected scan accepted"
        ));
        DepartmentAuditEntity department = database.departmentAuditDao()
                .getDepartment(request.getSessionId(), request.getSelectedDepartment());
        int scannedCount = department == null ? 1 : department.scannedCount + 1;
        int remaining = database.assetDao().countByDepartmentAndStatus(
                request.getSessionId(),
                request.getSelectedDepartment(),
                AuditStatus.NOT_AUDITED
        );
        DepartmentAuditStatus status = remaining == 0 ? DepartmentAuditStatus.COMPLETE : DepartmentAuditStatus.IN_PROGRESS;
        Long completedAt = remaining == 0 ? now : null;
        int departmentUpdated = database.departmentAuditDao().updateProgress(
                request.getSessionId(),
                request.getSelectedDepartment(),
                scannedCount,
                status,
                completedAt
        );
        if (departmentUpdated != 1) {
            throw new IllegalStateException("Expected department update affected " + departmentUpdated + " rows.");
        }
        return ScanProcessingResult.successExpected(asset.assetTagId, request.getSelectedDepartment(), asset.description, now);
    }

    private void insertRejectedEvent(ScanRequest request, String barcode, ScanResultType resultType,
                                     String assetDepartment, String description, String note, long now) {
        database.auditEventDao().insert(new AuditEventEntity(
                UUID.randomUUID().toString(),
                request.getSessionId(),
                barcode,
                EventKind.SCAN_REJECTED,
                resultType,
                request.getSelectedDepartment(),
                assetDepartment,
                now,
                userIdentityProvider.getDisplayName(),
                deviceInfoProvider.getModel(),
                note + (description == null ? "" : ": " + description)
        ));
    }

    private boolean safeEquals(String left, String right) {
        if (left == null) {
            return right == null;
        }
        return left.equals(right);
    }
}
