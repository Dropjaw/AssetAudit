package uk.co.hsim.assetaudit.scanner;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import android.os.Bundle;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.ArrayList;

@RunWith(AndroidJUnit4.class)
public class DataWedgeProfileManagerTests {
    @Test
    public void buildsSetConfigBundleForAssetAuditProfile() {
        DataWedgeProfileManager manager = new DataWedgeProfileManager(
                InstrumentationRegistry.getInstrumentation().getTargetContext());
        DataWedgeProfileConfig config = DataWedgeProfileConfig.defaults("uk.co.hsim.assetaudit");
        DataWedgeProfileSpec spec = manager.buildSpec(config);

        Bundle bundle = manager.buildSetConfigBundle(spec);

        assertEquals("AssetAudit_TC21", bundle.getString("PROFILE_NAME"));
        assertEquals("CREATE_IF_NOT_EXIST", bundle.getString("CONFIG_MODE"));
        ArrayList<Bundle> plugins = bundle.getParcelableArrayList("PLUGIN_CONFIG");
        assertEquals(3, plugins.size());
        assertTrue(findPlugin(plugins, "BARCODE").getBundle("PARAM_LIST").getString("scanner_input_enabled").equals("true"));
        assertFalse(Boolean.parseBoolean(findPlugin(plugins, "KEYSTROKE").getBundle("PARAM_LIST").getString("keystroke_output_enabled")));
        Bundle intentParams = findPlugin(plugins, "INTENT").getBundle("PARAM_LIST");
        assertEquals("true", intentParams.getString("intent_output_enabled"));
        assertEquals(DataWedgeConstants.ACTION_SCAN, intentParams.getString("intent_action"));
        assertEquals(DataWedgeConstants.CATEGORY_SCAN, intentParams.getString("intent_category"));
        assertEquals("2", intentParams.getString("intent_delivery"));
    }

    private Bundle findPlugin(ArrayList<Bundle> plugins, String name) {
        for (Bundle plugin : plugins) {
            if (name.equals(plugin.getString("PLUGIN_NAME"))) {
                return plugin;
            }
        }
        throw new AssertionError("Missing plugin " + name);
    }
}
