package uk.co.hsim.assetaudit.service;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import uk.co.hsim.assetaudit.scanner.DataWedgeConstants;

public class SettingsValidationServiceTests {
    private final SettingsValidationService service = new SettingsValidationService();

    @Test
    public void acceptsDefaultScannerAndExportSettings() {
        assertTrue(service.validate(
                "uk.co.hsim.assetaudit",
                "Unassigned / Blank Department",
                "CSV",
                DataWedgeConstants.PROFILE_NAME,
                DataWedgeConstants.ACTION_SCAN,
                DataWedgeConstants.CATEGORY_SCAN
        ).isSuccess());
    }

    @Test
    public void rejectsUnsafeSettings() {
        assertFalse(service.validate(
                "uk.co.hsim.assetaudit",
                " ",
                "XLSX",
                "",
                "other.app.SCAN",
                "bad.category"
        ).isSuccess());
    }
}
