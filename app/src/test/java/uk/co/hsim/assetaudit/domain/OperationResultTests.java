package uk.co.hsim.assetaudit.domain;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import uk.co.hsim.assetaudit.domain.results.ErrorCode;
import uk.co.hsim.assetaudit.domain.results.OperationResult;

public class OperationResultTests {
    @Test
    public void okResultCarriesValue() {
        OperationResult<String> result = OperationResult.ok("ready");
        assertTrue(result.isSuccess());
        assertEquals("ready", result.getValue());
        assertNull(result.getErrorCode());
    }

    @Test
    public void failureResultCarriesErrorDetails() {
        OperationResult<String> result = OperationResult.fail(ErrorCode.FEATURE_NOT_AVAILABLE, "Phase 2");
        assertFalse(result.isSuccess());
        assertEquals(ErrorCode.FEATURE_NOT_AVAILABLE, result.getErrorCode());
        assertEquals("Phase 2", result.getMessage());
    }
}
