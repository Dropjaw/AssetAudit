package uk.co.hsim.assetaudit.util.logging;

import android.util.Log;

public final class AndroidAppLogger implements AppLogger {
    private static final String TAG = "AssetAudit";

    @Override
    public void info(String component, String message) {
        Log.i(TAG, component + ": " + message);
    }

    @Override
    public void warning(String component, String message) {
        Log.w(TAG, component + ": " + message);
    }

    @Override
    public void error(String component, String message, Throwable throwable) {
        Log.e(TAG, component + ": " + message, throwable);
    }
}
