package uk.co.hsim.assetaudit.hardening;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import uk.co.hsim.assetaudit.scanner.ScannerPayload;

public class DiagnosticRedactorTests {
    private final DiagnosticRedactor redactor = new DiagnosticRedactor();

    @Test
    public void messageRedactsUrisAndPaths() {
        String message = redactor.redactMessage("open content://docs/tree/private-data and C:\\Users\\name\\file.csv");

        assertFalse(message.contains("content://docs"));
        assertFalse(message.contains("C:\\Users"));
        assertTrue(message.contains("content://[redacted]"));
        assertTrue(message.contains("[path-redacted]"));
    }

    @Test
    public void assetTagKeepsSuffixAndHashButNotFullValue() {
        String tag = redactor.redactAssetTag("00123456789");

        assertTrue(tag.contains("6789"));
        assertFalse(tag.contains("00123456789"));
    }

    @Test
    public void scannerSummaryDoesNotExposeRawBarcode() {
        String summary = redactor.scannerPayloadSummary(new ScannerPayload(
                "00123456789", "LABEL-TYPE-CODE128", 1L, "scanner"));

        assertFalse(summary.contains("00123456789"));
        assertTrue(summary.contains("LABEL-TYPE-CODE128"));
    }
}
