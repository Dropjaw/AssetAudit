package uk.co.hsim.assetaudit.ui.navigation;

public enum Screen {
    HOME("Home"),
    IMPORT_FILE("Import File"),
    DEPARTMENTS("Departments"),
    AUDIT_SCAN("Audit Scan"),
    REPORTS("Reports"),
    SETTINGS("Settings"),
    ABOUT("About / Diagnostics");

    private final String title;

    Screen(String title) {
        this.title = title;
    }

    public String getTitle() {
        return title;
    }
}
