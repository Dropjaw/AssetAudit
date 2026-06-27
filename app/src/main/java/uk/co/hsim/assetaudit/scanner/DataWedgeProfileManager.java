package uk.co.hsim.assetaudit.scanner;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import java.util.ArrayList;

public final class DataWedgeProfileManager {
    private final Context context;

    public DataWedgeProfileManager(Context context) {
        this.context = context.getApplicationContext();
    }

    public DataWedgeProfileSpec buildSpec(DataWedgeProfileConfig config) {
        return new DataWedgeProfileSpec(config, true, false, true, "CREATE_IF_NOT_EXIST", "2");
    }

    public String configureProfile(DataWedgeProfileConfig config) {
        Intent intent = new Intent(DataWedgeConstants.ACTION_DATAWEDGE_API);
        intent.putExtra(DataWedgeConstants.EXTRA_SET_CONFIG, buildSetConfigBundle(buildSpec(config)));
        intent.putExtra(DataWedgeConstants.EXTRA_SEND_RESULT, DataWedgeConstants.SEND_RESULT_LAST_RESULT);
        intent.putExtra(DataWedgeConstants.EXTRA_COMMAND_IDENTIFIER, DataWedgeConstants.COMMAND_CONFIGURE_PROFILE);
        context.sendBroadcast(intent);
        return "Requested DataWedge profile setup for " + config.getProfileName();
    }

    Bundle buildSetConfigBundle(DataWedgeProfileSpec spec) {
        DataWedgeProfileConfig config = spec.getConfig();
        Bundle profile = new Bundle();
        profile.putString("PROFILE_NAME", config.getProfileName());
        profile.putString("PROFILE_ENABLED", "true");
        profile.putString("CONFIG_MODE", spec.getConfigMode());
        profile.putParcelableArrayList("APP_LIST", appList(config));

        ArrayList<Bundle> pluginConfig = new ArrayList<>();
        pluginConfig.add(barcodePlugin(spec));
        pluginConfig.add(keystrokePlugin(spec));
        pluginConfig.add(intentPlugin(spec));
        profile.putParcelableArrayList("PLUGIN_CONFIG", pluginConfig);
        return profile;
    }

    private ArrayList<Bundle> appList(DataWedgeProfileConfig config) {
        Bundle app = new Bundle();
        app.putString("PACKAGE_NAME", config.getPackageName());
        app.putStringArray("ACTIVITY_LIST", new String[]{"*"});
        ArrayList<Bundle> apps = new ArrayList<>();
        apps.add(app);
        return apps;
    }

    private Bundle barcodePlugin(DataWedgeProfileSpec spec) {
        Bundle params = new Bundle();
        Bundle plugin = new Bundle();
        plugin.putString("PLUGIN_NAME", "BARCODE");
        plugin.putString("RESET_CONFIG", "true");
        params.putString("scanner_selection", "auto");
        params.putString("scanner_input_enabled", String.valueOf(spec.isBarcodeInputEnabled()));
        plugin.putBundle("PARAM_LIST", params);
        return plugin;
    }

    private Bundle keystrokePlugin(DataWedgeProfileSpec spec) {
        Bundle params = new Bundle();
        Bundle plugin = new Bundle();
        plugin.putString("PLUGIN_NAME", "KEYSTROKE");
        plugin.putString("RESET_CONFIG", "true");
        params.putString("keystroke_output_enabled", String.valueOf(spec.isKeystrokeOutputEnabled()));
        plugin.putBundle("PARAM_LIST", params);
        return plugin;
    }

    private Bundle intentPlugin(DataWedgeProfileSpec spec) {
        DataWedgeProfileConfig config = spec.getConfig();
        Bundle params = new Bundle();
        Bundle plugin = new Bundle();
        plugin.putString("PLUGIN_NAME", "INTENT");
        plugin.putString("RESET_CONFIG", "true");
        params.putString("intent_output_enabled", String.valueOf(spec.isIntentOutputEnabled()));
        params.putString("intent_action", config.getIntentAction());
        params.putString("intent_category", config.getIntentCategory());
        params.putString("intent_delivery", spec.getIntentDelivery());
        plugin.putBundle("PARAM_LIST", params);
        return plugin;
    }
}
