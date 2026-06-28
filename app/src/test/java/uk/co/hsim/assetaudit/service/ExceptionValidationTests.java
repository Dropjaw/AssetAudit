package uk.co.hsim.assetaudit.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.util.Collections;

import uk.co.hsim.assetaudit.domain.results.OperationResult;

public class ExceptionValidationTests {
    private final ExceptionResolutionService service = new ExceptionResolutionService(
            null, null, null, null, null, null);

    @Test
    public void newAssetValidationRequiresCoreFields() {
        assertEquals("Description is required.", service.validateNewAssetDraft(new NewAssetDraft(
                "session", "0011", "IT", "", "Available", "Main", "",
                "Computer equipment", "", "", "", "")));
        assertEquals("Status is required.", service.validateNewAssetDraft(new NewAssetDraft(
                "session", "0011", "IT", "Monitor", "", "Main", "",
                "Computer equipment", "", "", "", "")));
        assertEquals("Site is required.", service.validateNewAssetDraft(new NewAssetDraft(
                "session", "0011", "IT", "Monitor", "Available", "", "",
                "Computer equipment", "", "", "", "")));
        assertEquals("Category is required.", service.validateNewAssetDraft(new NewAssetDraft(
                "session", "0011", "IT", "Monitor", "Available", "Main", "",
                "", "", "", "", "")));
    }

    @Test
    public void newAssetValidationPreservesLeadingZeroesAndAlphanumericTags() {
        NewAssetDraft leadingZero = validDraft("0011");
        NewAssetDraft alphanumeric = validDraft("NN93N061612303C1F");

        assertNull(service.validateNewAssetDraft(leadingZero));
        assertNull(service.validateNewAssetDraft(alphanumeric));
        assertEquals("0011", leadingZero.getAssetTagId());
        assertEquals("NN93N061612303C1F", alphanumeric.getAssetTagId());
    }

    @Test
    public void skipValidationBlocksBlankReasonBeforeDatabaseWork() {
        OperationResult<ExceptionResolutionResult> result = service.skipAssets(new SkipAssetsRequest(
                "session", "IT", Collections.singletonList("0011"), " "));

        assertTrue(!result.isSuccess());
        assertEquals("Skip reason is required.", result.getMessage());
    }

    private NewAssetDraft validDraft(String tag) {
        return new NewAssetDraft(
                "session",
                tag,
                "IT",
                "Monitor",
                "Available",
                "Main",
                "",
                "Computer equipment",
                "",
                "",
                "",
                ""
        );
    }
}
