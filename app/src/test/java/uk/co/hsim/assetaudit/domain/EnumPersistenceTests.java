package uk.co.hsim.assetaudit.domain;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import uk.co.hsim.assetaudit.domain.enums.AuditStatus;
import uk.co.hsim.assetaudit.domain.enums.DepartmentAuditStatus;
import uk.co.hsim.assetaudit.domain.enums.EventKind;
import uk.co.hsim.assetaudit.domain.enums.ImportIssueSeverity;
import uk.co.hsim.assetaudit.domain.enums.ScanResultType;
import uk.co.hsim.assetaudit.domain.enums.SessionStatus;

public class EnumPersistenceTests {
    @Test
    public void enumNamesRoundTripForDatabaseStorage() {
        assertEquals(AuditStatus.NOT_AUDITED, AuditStatus.valueOf(AuditStatus.NOT_AUDITED.name()));
        assertEquals(ScanResultType.SUCCESS_EXPECTED, ScanResultType.valueOf(ScanResultType.SUCCESS_EXPECTED.name()));
        assertEquals(SessionStatus.ACTIVE, SessionStatus.valueOf(SessionStatus.ACTIVE.name()));
        assertEquals(DepartmentAuditStatus.IN_PROGRESS, DepartmentAuditStatus.valueOf(DepartmentAuditStatus.IN_PROGRESS.name()));
        assertEquals(ImportIssueSeverity.FATAL, ImportIssueSeverity.valueOf(ImportIssueSeverity.FATAL.name()));
        assertEquals(EventKind.SESSION_CREATED, EventKind.valueOf(EventKind.SESSION_CREATED.name()));
    }
}
