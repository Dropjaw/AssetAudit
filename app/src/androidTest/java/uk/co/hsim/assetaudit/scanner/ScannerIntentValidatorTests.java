package uk.co.hsim.assetaudit.scanner;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import android.content.Intent;

import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class ScannerIntentValidatorTests {
    private final ScannerIntentValidator validator = new ScannerIntentValidator();

    @Test
    public void acceptsExpectedDataWedgeIntent() {
        Intent intent = baseIntent();
        intent.putExtra(DataWedgeConstants.EXTRA_DATA_STRING, "0011");

        assertTrue(validator.validate(intent).isValid());
    }

    @Test
    public void rejectsMissingCategoryUnexpectedExtraAndOverlongPayload() {
        Intent missingCategory = new Intent(DataWedgeConstants.ACTION_SCAN);
        missingCategory.putExtra(DataWedgeConstants.EXTRA_DATA_STRING, "0011");
        assertFalse(validator.validate(missingCategory).isValid());

        Intent unexpectedExtra = baseIntent();
        unexpectedExtra.putExtra(DataWedgeConstants.EXTRA_DATA_STRING, "0011");
        unexpectedExtra.putExtra("unexpected", "value");
        assertFalse(validator.validate(unexpectedExtra).isValid());

        Intent overlong = baseIntent();
        overlong.putExtra(DataWedgeConstants.EXTRA_DATA_STRING,
                "12345678901234567890123456789012345678901234567890123456789012345678901234567890"
                        + "123456789012345678901234567890123456789012345678901234567890");
        assertFalse(validator.validate(overlong).isValid());
    }

    private Intent baseIntent() {
        Intent intent = new Intent(DataWedgeConstants.ACTION_SCAN);
        intent.addCategory(DataWedgeConstants.CATEGORY_SCAN);
        return intent;
    }
}
