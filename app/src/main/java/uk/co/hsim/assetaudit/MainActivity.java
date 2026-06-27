package uk.co.hsim.assetaudit;

import android.graphics.Typeface;
import android.os.Bundle;
import android.view.Gravity;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatActivity;

import java.text.DateFormat;
import java.util.Date;
import java.util.List;

import uk.co.hsim.assetaudit.app.AppContainer;
import uk.co.hsim.assetaudit.data.db.AuditDatabase;
import uk.co.hsim.assetaudit.data.entity.AuditSessionEntity;
import uk.co.hsim.assetaudit.data.entity.DiagnosticLogEntity;
import uk.co.hsim.assetaudit.service.SettingsKeys;
import uk.co.hsim.assetaudit.ui.navigation.AppNavigator;
import uk.co.hsim.assetaudit.ui.navigation.Screen;

public class MainActivity extends AppCompatActivity {
    private AppContainer appContainer;
    private final AppNavigator navigator = new AppNavigator();
    private LinearLayout root;
    private TextView titleView;
    private TextView statusView;
    private LinearLayout content;
    private AuditSessionEntity activeSession;
    private List<DiagnosticLogEntity> recentDiagnostics;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        appContainer = AppContainer.get(this);
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (navigator.canGoBack()) {
                    navigator.goBack();
                    renderCurrentScreen();
                } else {
                    setEnabled(false);
                    getOnBackPressedDispatcher().onBackPressed();
                }
            }
        });
        buildRoot();
        renderLoading();
        appContainer.executors.diskIO().execute(() -> {
            appContainer.appStartupService.initialiseApplication();
            activeSession = appContainer.auditSessionService.getActiveSession();
            recentDiagnostics = appContainer.diagnosticService.listRecent(8);
            appContainer.executors.mainThread(this::renderCurrentScreen);
        });
    }

    private void buildRoot() {
        root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(20), dp(36), dp(20), dp(20));
        root.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
        ));

        titleView = new TextView(this);
        titleView.setTextSize(24);
        titleView.setTypeface(Typeface.DEFAULT_BOLD);
        titleView.setGravity(Gravity.START);
        root.addView(titleView, matchWrap());

        statusView = new TextView(this);
        statusView.setTextSize(14);
        statusView.setPadding(0, dp(4), 0, dp(12));
        root.addView(statusView, matchWrap());

        ScrollView scrollView = new ScrollView(this);
        content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        scrollView.addView(content, new ScrollView.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        ));
        root.addView(scrollView, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                0,
                1f
        ));

        setContentView(root);
    }

    private void renderLoading() {
        titleView.setText(getString(R.string.app_name));
        statusView.setText("Opening local audit database...");
        content.removeAllViews();
    }

    private void renderCurrentScreen() {
        Screen screen = navigator.getCurrent();
        titleView.setText(screen.getTitle());
        statusView.setText(activeSession == null
                ? "No active audit session. Import is planned for Phase 2."
                : "Active session: " + activeSession.auditName);
        content.removeAllViews();

        switch (screen) {
            case HOME:
                renderHome();
                break;
            case IMPORT_FILE:
                renderPlaceholder(
                        "Asset file import",
                        "Phase 2 will add the Android document picker, CSV/XLSX parsing, validation, and transactional session creation.",
                        "Choose file is intentionally unavailable in Phase 1."
                );
                break;
            case DEPARTMENTS:
                renderPlaceholder(
                        "Department dashboard",
                        "Department progress will appear here after an asset file has created an active local session.",
                        "No imported session is available yet."
                );
                break;
            case AUDIT_SCAN:
                renderPlaceholder(
                        "Scan workspace",
                        "Live Zebra DataWedge capture, manual entry, and scan classification are reserved for later phases.",
                        "No scan state is mutated in Phase 1."
                );
                break;
            case REPORTS:
                renderPlaceholder(
                        "Reports and export",
                        "Updated asset files, summaries, and exception reports will be generated after import and audit workflows exist.",
                        "Export buttons are disabled in Phase 1."
                );
                break;
            case SETTINGS:
                renderSettings();
                break;
            case ABOUT:
                renderAbout();
                break;
            default:
                renderPlaceholder("Unknown screen", "The requested route is not available.", "Return Home.");
        }
    }

    private void renderHome() {
        addBody("Asset Audit is ready for the foundation phase. The local database, settings, diagnostics, and navigation shell are active.");
        addNavButton("Import Asset File", Screen.IMPORT_FILE, true);
        addNavButton("Departments", Screen.DEPARTMENTS, activeSession != null);
        addNavButton("Audit Scan", Screen.AUDIT_SCAN, activeSession != null);
        addNavButton("Reports", Screen.REPORTS, activeSession != null);
        addNavButton("Settings", Screen.SETTINGS, true);
        addNavButton("About / Diagnostics", Screen.ABOUT, true);
    }

    private void renderPlaceholder(String heading, String body, String disabledReason) {
        addSectionHeading(heading);
        addBody(body);
        Button unavailable = button("Phase 2+ feature");
        unavailable.setEnabled(false);
        content.addView(unavailable, matchWrap());
        addBody(disabledReason);
        addBackButton();
    }

    private void renderSettings() {
        addSectionHeading("Audit workflow defaults");
        CheckBox movement = checkbox("Confirm before moving assets between departments");
        CheckBox manual = checkbox("Allow manual barcode entry");
        CheckBox newAsset = checkbox("Allow new asset creation");
        CheckBox diagnostics = checkbox("Enable local diagnostic logging");
        EditText unassigned = editText("Unassigned department label");
        EditText exportFormat = editText("Default export format");

        content.addView(movement);
        content.addView(manual);
        content.addView(newAsset);
        content.addView(diagnostics);
        content.addView(unassigned, matchWrap());
        content.addView(exportFormat, matchWrap());

        appContainer.executors.diskIO().execute(() -> {
            boolean movementValue = appContainer.settingsService.getBooleanSetting(SettingsKeys.REQUIRE_MOVEMENT_CONFIRMATION, true);
            boolean manualValue = appContainer.settingsService.getBooleanSetting(SettingsKeys.ALLOW_MANUAL_BARCODE_ENTRY, true);
            boolean newAssetValue = appContainer.settingsService.getBooleanSetting(SettingsKeys.ALLOW_NEW_ASSET_CREATION, true);
            boolean diagnosticsValue = appContainer.settingsService.getBooleanSetting(SettingsKeys.DIAGNOSTIC_LOGGING_ENABLED, true);
            String unassignedValue = appContainer.settingsService.getStringSetting(SettingsKeys.UNASSIGNED_DEPARTMENT_LABEL, "Unassigned / Blank Department");
            String exportValue = appContainer.settingsService.getStringSetting(SettingsKeys.DEFAULT_EXPORT_FORMAT, "CSV");
            appContainer.executors.mainThread(() -> {
                movement.setChecked(movementValue);
                manual.setChecked(manualValue);
                newAsset.setChecked(newAssetValue);
                diagnostics.setChecked(diagnosticsValue);
                unassigned.setText(unassignedValue);
                exportFormat.setText(exportValue);
            });
        });

        Button save = button("Save settings");
        save.setOnClickListener(v -> {
            boolean movementValue = movement.isChecked();
            boolean manualValue = manual.isChecked();
            boolean newAssetValue = newAsset.isChecked();
            boolean diagnosticsValue = diagnostics.isChecked();
            String unassignedValue = unassigned.getText().toString();
            String exportValue = exportFormat.getText().toString();
            appContainer.executors.diskIO().execute(() -> {
            appContainer.settingsService.setBooleanSetting(SettingsKeys.REQUIRE_MOVEMENT_CONFIRMATION, movementValue);
            appContainer.settingsService.setBooleanSetting(SettingsKeys.ALLOW_MANUAL_BARCODE_ENTRY, manualValue);
            appContainer.settingsService.setBooleanSetting(SettingsKeys.ALLOW_NEW_ASSET_CREATION, newAssetValue);
            appContainer.settingsService.setBooleanSetting(SettingsKeys.DIAGNOSTIC_LOGGING_ENABLED, diagnosticsValue);
            appContainer.settingsService.setStringSetting(SettingsKeys.UNASSIGNED_DEPARTMENT_LABEL, unassignedValue);
            appContainer.settingsService.setStringSetting(SettingsKeys.DEFAULT_EXPORT_FORMAT, exportValue);
            appContainer.diagnosticService.logInfo("Settings", "Foundation settings saved");
            recentDiagnostics = appContainer.diagnosticService.listRecent(8);
            appContainer.executors.mainThread(() -> Toast.makeText(this, "Settings saved", Toast.LENGTH_SHORT).show());
            });
        });
        content.addView(save, matchWrap());
        addBackButton();
    }

    private void renderAbout() {
        addSectionHeading("Application");
        addKeyValue("Version", appContainer.deviceInfoProvider.getAppVersionName()
                + " (" + appContainer.deviceInfoProvider.getAppVersionCode() + ")");
        addKeyValue("Package", appContainer.deviceInfoProvider.getPackageName());
        addKeyValue("Activity", appContainer.deviceInfoProvider.getMainActivityName());
        addKeyValue("Database", AuditDatabase.DATABASE_NAME + " v" + AuditDatabase.DATABASE_VERSION);

        addSectionHeading("Device");
        addKeyValue("Manufacturer", appContainer.deviceInfoProvider.getManufacturer());
        addKeyValue("Model", appContainer.deviceInfoProvider.getModel());
        addKeyValue("Android", appContainer.deviceInfoProvider.getAndroidRelease()
                + " API " + appContainer.deviceInfoProvider.getSdkInt());

        addSectionHeading("Recent diagnostics");
        if (recentDiagnostics == null || recentDiagnostics.isEmpty()) {
            addBody("No diagnostic entries yet.");
        } else {
            DateFormat dateFormat = DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.MEDIUM);
            for (DiagnosticLogEntity log : recentDiagnostics) {
                addBody(dateFormat.format(new Date(log.timestampUtc)) + " - " + log.level
                        + " - " + log.component + ": " + log.message);
            }
        }
        addBackButton();
    }

    private void addNavButton(String text, Screen screen, boolean enabled) {
        Button button = button(text);
        button.setEnabled(enabled);
        button.setOnClickListener(v -> {
            navigator.navigateTo(screen);
            renderCurrentScreen();
        });
        content.addView(button, matchWrap());
    }

    private void addBackButton() {
        Button back = button("Back");
        back.setOnClickListener(v -> {
            navigator.goBack();
            renderCurrentScreen();
        });
        content.addView(back, matchWrap());
    }

    private void addSectionHeading(String text) {
        TextView view = new TextView(this);
        view.setText(text);
        view.setTextSize(18);
        view.setTypeface(Typeface.DEFAULT_BOLD);
        view.setPadding(0, dp(16), 0, dp(6));
        content.addView(view, matchWrap());
    }

    private void addBody(String text) {
        TextView view = new TextView(this);
        view.setText(text);
        view.setTextSize(15);
        view.setPadding(0, dp(4), 0, dp(8));
        content.addView(view, matchWrap());
    }

    private void addKeyValue(String key, String value) {
        addBody(key + ": " + value);
    }

    private Button button(String text) {
        Button button = new Button(this);
        button.setText(text);
        button.setAllCaps(false);
        button.setMinHeight(dp(48));
        return button;
    }

    private CheckBox checkbox(String text) {
        CheckBox checkBox = new CheckBox(this);
        checkBox.setText(text);
        checkBox.setTextSize(15);
        checkBox.setPadding(0, dp(4), 0, dp(4));
        return checkBox;
    }

    private EditText editText(String hint) {
        EditText editText = new EditText(this);
        editText.setHint(hint);
        editText.setSingleLine(true);
        return editText;
    }

    private LinearLayout.LayoutParams matchWrap() {
        return new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
    }

    private int dp(int value) {
        return (int) (value * getResources().getDisplayMetrics().density);
    }
}
