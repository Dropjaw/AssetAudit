package uk.co.hsim.assetaudit.data;

import uk.co.hsim.assetaudit.data.entity.AppSettingEntity;
import uk.co.hsim.assetaudit.data.entity.AssetEntity;
import uk.co.hsim.assetaudit.data.entity.AuditEventEntity;
import uk.co.hsim.assetaudit.data.entity.AuditSessionEntity;
import uk.co.hsim.assetaudit.data.entity.DepartmentAuditEntity;
import uk.co.hsim.assetaudit.domain.enums.AuditStatus;
import uk.co.hsim.assetaudit.domain.enums.DepartmentAuditStatus;
import uk.co.hsim.assetaudit.domain.enums.EventKind;
import uk.co.hsim.assetaudit.domain.enums.ScanResultType;
import uk.co.hsim.assetaudit.domain.enums.SessionStatus;

final class TestEntities {
    private TestEntities() {
    }

    static AuditSessionEntity session(String sessionId) {
        return new AuditSessionEntity(
                sessionId,
                "Test audit " + sessionId,
                null,
                null,
                1000L,
                null,
                SessionStatus.ACTIVE,
                "tester",
                "device",
                1
        );
    }

    static AssetEntity asset(String sessionId, String assetTagId, String department) {
        return new AssetEntity(
                sessionId,
                assetTagId,
                department,
                null,
                "Monitor",
                "Available",
                "Main",
                "Unit 11",
                "Computer equipment",
                "Monitor",
                null,
                null,
                AuditStatus.NOT_AUDITED,
                false,
                2000L
        );
    }

    static AuditEventEntity event(String eventId, String sessionId, String assetTagId, long timestampUtc) {
        return new AuditEventEntity(
                eventId,
                sessionId,
                assetTagId,
                EventKind.SCAN_ACCEPTED,
                ScanResultType.SUCCESS_EXPECTED,
                "IT",
                null,
                timestampUtc,
                "tester",
                "device",
                null
        );
    }

    static DepartmentAuditEntity department(String sessionId, String departmentName) {
        return new DepartmentAuditEntity(
                sessionId,
                departmentName,
                1,
                0,
                0,
                0,
                0,
                DepartmentAuditStatus.NOT_STARTED,
                null
        );
    }

    static AppSettingEntity setting(String key, String value) {
        return new AppSettingEntity(key, value, 3000L);
    }
}
