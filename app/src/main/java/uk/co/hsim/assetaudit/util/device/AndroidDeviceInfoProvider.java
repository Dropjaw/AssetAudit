package uk.co.hsim.assetaudit.util.device;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Build;

import uk.co.hsim.assetaudit.MainActivity;

public final class AndroidDeviceInfoProvider implements DeviceInfoProvider {
    private final Context context;

    public AndroidDeviceInfoProvider(Context context) {
        this.context = context.getApplicationContext();
    }

    @Override
    public String getManufacturer() {
        return Build.MANUFACTURER;
    }

    @Override
    public String getModel() {
        return Build.MODEL;
    }

    @Override
    public String getAndroidRelease() {
        return Build.VERSION.RELEASE;
    }

    @Override
    public int getSdkInt() {
        return Build.VERSION.SDK_INT;
    }

    @Override
    public String getPackageName() {
        return context.getPackageName();
    }

    @Override
    public String getMainActivityName() {
        return MainActivity.class.getName();
    }

    @Override
    public String getAppVersionName() {
        try {
            PackageInfo packageInfo = context.getPackageManager().getPackageInfo(context.getPackageName(), 0);
            return packageInfo.versionName == null ? "unknown" : packageInfo.versionName;
        } catch (PackageManager.NameNotFoundException e) {
            return "unknown";
        }
    }

    @Override
    public long getAppVersionCode() {
        try {
            PackageInfo packageInfo = context.getPackageManager().getPackageInfo(context.getPackageName(), 0);
            return packageInfo.getLongVersionCode();
        } catch (PackageManager.NameNotFoundException e) {
            return 0L;
        }
    }
}
