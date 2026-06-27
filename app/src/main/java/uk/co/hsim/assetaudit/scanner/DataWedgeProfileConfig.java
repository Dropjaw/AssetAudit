package uk.co.hsim.assetaudit.scanner;

public final class DataWedgeProfileConfig {
    private final String profileName;
    private final String packageName;
    private final String intentAction;
    private final String intentCategory;

    public DataWedgeProfileConfig(String profileName, String packageName, String intentAction, String intentCategory) {
        this.profileName = profileName;
        this.packageName = packageName;
        this.intentAction = intentAction;
        this.intentCategory = intentCategory;
    }

    public static DataWedgeProfileConfig defaults(String packageName) {
        return new DataWedgeProfileConfig(
                DataWedgeConstants.PROFILE_NAME,
                packageName,
                DataWedgeConstants.ACTION_SCAN,
                DataWedgeConstants.CATEGORY_SCAN
        );
    }

    public String getProfileName() {
        return profileName;
    }

    public String getPackageName() {
        return packageName;
    }

    public String getIntentAction() {
        return intentAction;
    }

    public String getIntentCategory() {
        return intentCategory;
    }
}
