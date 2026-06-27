package uk.co.hsim.assetaudit.scanner;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

public class ScannerPayloadParserTests {
    private final ScannerPayloadParser parser = new ScannerPayloadParser();

    @Test
    public void validDataWedgePayloadPreservesLeadingZeroes() {
        Map<String, String> extras = new HashMap<>();
        extras.put(DataWedgeConstants.EXTRA_DATA_STRING, "0011");
        extras.put(DataWedgeConstants.EXTRA_LABEL_TYPE, "LABEL-TYPE-CODE128");
        extras.put(DataWedgeConstants.EXTRA_SOURCE, "scanner");

        ScannerPayloadParser.ParseResult result = parser.parse(DataWedgeConstants.ACTION_SCAN, extras, 1000L);

        assertTrue(result.isAccepted());
        assertEquals("0011", result.getPayload().getData());
        assertEquals("LABEL-TYPE-CODE128", result.getPayload().getSymbology());
        assertEquals("scanner", result.getPayload().getSource());
    }

    @Test
    public void wrongActionIsIgnored() {
        Map<String, String> extras = new HashMap<>();
        extras.put(DataWedgeConstants.EXTRA_DATA_STRING, "0011");

        ScannerPayloadParser.ParseResult result = parser.parse("wrong.action", extras, 1000L);

        assertFalse(result.isAccepted());
        assertEquals("Unexpected action", result.getReason());
    }

    @Test
    public void missingOrBlankDataStringIsIgnored() {
        Map<String, String> missing = new HashMap<>();
        Map<String, String> blank = new HashMap<>();
        blank.put(DataWedgeConstants.EXTRA_DATA_STRING, " ");

        assertFalse(parser.parse(DataWedgeConstants.ACTION_SCAN, missing, 1000L).isAccepted());
        assertFalse(parser.parse(DataWedgeConstants.ACTION_SCAN, blank, 1000L).isAccepted());
    }
}
