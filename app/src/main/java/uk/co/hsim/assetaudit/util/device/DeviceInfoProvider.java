package uk.co.hsim.assetaudit.util.device;

public interface DeviceInfoProvider {
    String getManufacturer();

    String getModel();

    String getAndroidRelease();

    int getSdkInt();

    String getPackageName();

    String getMainActivityName();

    String getAppVersionName();

    long getAppVersionCode();
}
