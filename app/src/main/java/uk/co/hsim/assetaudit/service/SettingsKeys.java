package uk.co.hsim.assetaudit.service;

public final class SettingsKeys {
    public static final String REQUIRE_MOVEMENT_CONFIRMATION = "require_movement_confirmation";
    public static final String ALLOW_MANUAL_BARCODE_ENTRY = "allow_manual_barcode_entry";
    public static final String ALLOW_NEW_ASSET_CREATION = "allow_new_asset_creation";
    public static final String UNASSIGNED_DEPARTMENT_LABEL = "unassigned_department_label";
    public static final String DEFAULT_EXPORT_FORMAT = "default_export_format";
    public static final String DIAGNOSTIC_LOGGING_ENABLED = "diagnostic_logging_enabled";
    public static final String DATABASE_CREATED_AT_UTC = "database_created_at_utc";
    public static final String LIVE_SCANNER_ENABLED = "live_scanner_enabled";
    public static final String DATAWEDGE_PROFILE_NAME = "datawedge_profile_name";
    public static final String DATAWEDGE_INTENT_ACTION = "datawedge_intent_action";
    public static final String DATAWEDGE_INTENT_CATEGORY = "datawedge_intent_category";
    public static final String DATAWEDGE_DEBOUNCE_MS = "datawedge_debounce_ms";
    public static final String SCANNER_DIAGNOSTICS_ENABLED = "scanner_diagnostics_enabled";

    private SettingsKeys() {
    }
}
