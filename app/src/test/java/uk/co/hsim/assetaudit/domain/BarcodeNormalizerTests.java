package uk.co.hsim.assetaudit.domain;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import uk.co.hsim.assetaudit.domain.rules.BarcodeNormalizer;

public class BarcodeNormalizerTests {
    @Test
    public void nullReturnsBlank() {
        assertEquals("", BarcodeNormalizer.normalizeAssetTag(null));
    }

    @Test
    public void outerWhitespaceTrimmedAndLeadingZeroesPreserved() {
        assertEquals("0011", BarcodeNormalizer.normalizeAssetTag(" 0011 "));
        assertEquals("0069", BarcodeNormalizer.normalizeAssetTag("0069"));
    }

    @Test
    public void blankAndOverlongTagsRejected() {
        assertFalse(BarcodeNormalizer.isValidBasicAssetTag(" "));
        assertTrue(BarcodeNormalizer.isValidBasicAssetTag("NN93N061612303C1F"));
        assertFalse(BarcodeNormalizer.isValidBasicAssetTag(
                "1234567890123456789012345678901234567890123456789012345678901234567890"
                        + "123456789012345678901234567890123456789012345678901234567890123456789"
        ));
    }
}
