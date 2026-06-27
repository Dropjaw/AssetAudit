package uk.co.hsim.assetaudit.data.db;

import androidx.room.TypeConverter;

import uk.co.hsim.assetaudit.domain.enums.AuditStatus;
import uk.co.hsim.assetaudit.domain.enums.DepartmentAuditStatus;
import uk.co.hsim.assetaudit.domain.enums.EventKind;
import uk.co.hsim.assetaudit.domain.enums.ImportIssueSeverity;
import uk.co.hsim.assetaudit.domain.enums.ScanResultType;
import uk.co.hsim.assetaudit.domain.enums.SessionStatus;

public final class AuditTypeConverters {
    @TypeConverter
    public String fromAuditStatus(AuditStatus value) {
        return value == null ? null : value.name();
    }

    @TypeConverter
    public AuditStatus toAuditStatus(String value) {
        return value == null ? null : AuditStatus.valueOf(value);
    }

    @TypeConverter
    public String fromSessionStatus(SessionStatus value) {
        return value == null ? null : value.name();
    }

    @TypeConverter
    public SessionStatus toSessionStatus(String value) {
        return value == null ? null : SessionStatus.valueOf(value);
    }

    @TypeConverter
    public String fromDepartmentAuditStatus(DepartmentAuditStatus value) {
        return value == null ? null : value.name();
    }

    @TypeConverter
    public DepartmentAuditStatus toDepartmentAuditStatus(String value) {
        return value == null ? null : DepartmentAuditStatus.valueOf(value);
    }

    @TypeConverter
    public String fromImportIssueSeverity(ImportIssueSeverity value) {
        return value == null ? null : value.name();
    }

    @TypeConverter
    public ImportIssueSeverity toImportIssueSeverity(String value) {
        return value == null ? null : ImportIssueSeverity.valueOf(value);
    }

    @TypeConverter
    public String fromEventKind(EventKind value) {
        return value == null ? null : value.name();
    }

    @TypeConverter
    public EventKind toEventKind(String value) {
        return value == null ? null : EventKind.valueOf(value);
    }

    @TypeConverter
    public String fromScanResultType(ScanResultType value) {
        return value == null ? null : value.name();
    }

    @TypeConverter
    public ScanResultType toScanResultType(String value) {
        return value == null ? null : ScanResultType.valueOf(value);
    }
}
