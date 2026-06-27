package uk.co.hsim.assetaudit;

import android.database.Cursor;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Bundle;
import android.provider.OpenableColumns;
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
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import java.text.DateFormat;
import java.util.Date;
import java.util.List;

import uk.co.hsim.assetaudit.app.AppContainer;
import uk.co.hsim.assetaudit.data.db.AuditDatabase;
import uk.co.hsim.assetaudit.data.entity.AuditSessionEntity;
import uk.co.hsim.assetaudit.data.entity.DepartmentAuditEntity;
import uk.co.hsim.assetaudit.data.entity.DiagnosticLogEntity;
import uk.co.hsim.assetaudit.domain.results.OperationResult;
import uk.co.hsim.assetaudit.importfile.AndroidDocumentSource;
import uk.co.hsim.assetaudit.importfile.AssetFileFormat;
import uk.co.hsim.assetaudit.importfile.AssetFileFormatDetector;
import uk.co.hsim.assetaudit.importfile.CreatedAuditSession;
import uk.co.hsim.assetaudit.importfile.DocumentReference;
import uk.co.hsim.assetaudit.importfile.ImportConfirmation;
import uk.co.hsim.assetaudit.importfile.ImportIssue;
import uk.co.hsim.assetaudit.importfile.ImportPreview;
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
    private ActivityResultLauncher<String[]> openAssetFileLauncher;
    private ImportPreview currentImportPreview;
    private CreatedAuditSession createdAuditSession;
    private String importError;
    private boolean importReading;
    private boolean importCreating;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        appContainer = AppContainer.get(this);
        openAssetFileLauncher = registerForActivityResult(new ActivityResultContracts.OpenDocument(), this::onAssetFileSelected);
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
                renderImportFile();
                break;
            case DEPARTMENTS:
                renderDepartments();
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

    private void renderImportFile() {
        if (createdAuditSession != null) {
            renderImportCreated();
            return;
        }
        if (activeSession != null && currentImportPreview == null && !importReading && !importCreating) {
            addSectionHeading("Active session exists");
            addBody("An audit session is already active. Phase 2 does not replace or archive active sessions.");
            Button departments = button("Open Departments");
            departments.setOnClickListener(v -> {
                navigator.navigateTo(Screen.DEPARTMENTS);
                renderCurrentScreen();
            });
            content.addView(departments, matchWrap());
            addBackButton();
            return;
        }
        if (importReading || importCreating) {
            addSectionHeading(importCreating ? "Creating local audit session" : "Reading asset file");
            addBody(importCreating ? "Writing assets, issues, lookups, and department summaries..." : "Parsing and validating the selected file...");
            return;
        }
        if (importError != null) {
            addSectionHeading("Import failed");
            addBody(importError);
            addChooseFileButton("Choose different file");
            addBackButton();
            return;
        }
        if (currentImportPreview == null) {
            addSectionHeading("Asset file import");
            addBody("Choose a CSV or XLSX asset file. The source file is opened read-only through Android document access.");
            addBody("Required columns: Asset Tag ID, Department, Description, Status, Site, Location, Category.");
            addChooseFileButton("Choose Asset File");
            addBackButton();
            return;
        }
        renderImportPreview();
    }

    private void renderDepartments() {
        if (activeSession == null) {
            renderPlaceholder(
                    "Department dashboard",
                    "Department progress will appear here after an asset file has created an active local session.",
                    "No imported session is available yet."
            );
            return;
        }
        addSectionHeading("Department dashboard");
        addBody("Imported department summaries are ready for the Phase 3 scan workflow.");
        appContainer.executors.diskIO().execute(() -> {
            List<DepartmentAuditEntity> departments = appContainer.departmentSummaryService
                    .getDepartmentSummaries(activeSession.sessionId);
            appContainer.executors.mainThread(() -> {
                content.removeAllViews();
                addSectionHeading("Department dashboard");
                if (departments.isEmpty()) {
                    addBody("No department summaries exist for the active session.");
                } else {
                    for (DepartmentAuditEntity department : departments) {
                        addBody(department.departmentName + ": " + department.expectedCount
                                + " expected, " + department.status);
                    }
                }
                addBackButton();
            });
        });
    }

    private void renderImportPreview() {
        addSectionHeading("Import preview");
        addKeyValue("File", currentImportPreview.getDocumentReference().getDisplayName());
        addKeyValue("Format", currentImportPreview.getDocumentReference().getDetectedFormat().name());
        addKeyValue("Rows read", String.valueOf(currentImportPreview.getParsedFile().getMetadata().getSourceRowCount()));
        addKeyValue("Accepted assets", String.valueOf(currentImportPreview.getValidationResult().getAcceptedRows().size()));
        addKeyValue("Fatal issues", String.valueOf(currentImportPreview.getValidationResult().getFatalCount()));
        addKeyValue("Warnings", String.valueOf(currentImportPreview.getValidationResult().getWarningCount()));
        addKeyValue("Info", String.valueOf(currentImportPreview.getValidationResult().getInfoCount()));
        addKeyValue("Leading-zero tags", String.valueOf(currentImportPreview.getLeadingZeroTagCount()));
        addKeyValue("Alphanumeric tags", String.valueOf(currentImportPreview.getAlphanumericTagCount()));

        addSectionHeading("Departments");
        int shown = 0;
        for (java.util.Map.Entry<String, Integer> entry : currentImportPreview.getDepartmentCounts().entrySet()) {
            addBody(entry.getKey() + ": " + entry.getValue());
            shown++;
            if (shown >= 8) {
                addBody("Additional departments: " + (currentImportPreview.getDepartmentCounts().size() - shown));
                break;
            }
        }

        addSectionHeading("Issues");
        if (currentImportPreview.getValidationResult().getIssues().isEmpty()) {
            addBody("No import issues found.");
        } else {
            for (ImportIssue issue : currentImportPreview.firstIssues(12)) {
                String row = issue.getRowNumber() == null ? "File" : "Row " + issue.getRowNumber();
                addBody(row + " - " + issue.getSeverity() + " - " + issue.getCode() + ": " + issue.getMessage());
            }
        }

        CheckBox confirmWarnings = null;
        if (currentImportPreview.getValidationResult().getWarningCount() > 0
                && currentImportPreview.getValidationResult().getFatalCount() == 0) {
            confirmWarnings = checkbox("I confirm the warnings and want to create the session");
            content.addView(confirmWarnings);
        }

        Button create = button(currentImportPreview.getValidationResult().getWarningCount() > 0
                ? "Create Session with Warnings"
                : "Create Session");
        create.setEnabled(currentImportPreview.getValidationResult().getFatalCount() == 0);
        CheckBox finalConfirmWarnings = confirmWarnings;
        create.setOnClickListener(v -> {
            boolean warningsAccepted = currentImportPreview.getValidationResult().getWarningCount() == 0
                    || (finalConfirmWarnings != null && finalConfirmWarnings.isChecked());
            if (!warningsAccepted) {
                Toast.makeText(this, "Confirm warnings before creating the session", Toast.LENGTH_SHORT).show();
                return;
            }
            createSessionFromPreview(warningsAccepted);
        });
        content.addView(create, matchWrap());
        addChooseFileButton("Choose different file");
        addBackButton();
    }

    private void renderImportCreated() {
        addSectionHeading("Session created");
        addKeyValue("Assets", String.valueOf(createdAuditSession.getAssetCount()));
        addKeyValue("Departments", String.valueOf(createdAuditSession.getDepartmentCount()));
        Button departments = button("Open Departments");
        departments.setOnClickListener(v -> {
            navigator.navigateTo(Screen.DEPARTMENTS);
            renderCurrentScreen();
        });
        content.addView(departments, matchWrap());
        addBackButton();
    }

    private void addChooseFileButton(String label) {
        Button choose = button(label);
        choose.setOnClickListener(v -> {
            appContainer.executors.diskIO().execute(() -> appContainer.diagnosticService.logInfo("Import", "IMPORT_PICKER_OPENED"));
            openAssetFileLauncher.launch(new String[]{
                    "text/csv",
                    "text/comma-separated-values",
                    "application/vnd.ms-excel",
                    "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
            });
        });
        content.addView(choose, matchWrap());
    }

    private void onAssetFileSelected(Uri uri) {
        if (uri == null) {
            return;
        }
        currentImportPreview = null;
        createdAuditSession = null;
        importError = null;
        importReading = true;
        renderCurrentScreen();
        appContainer.executors.diskIO().execute(() -> {
            DocumentReference reference = documentReferenceFor(uri);
            appContainer.diagnosticService.logInfo("Import", "IMPORT_FILE_SELECTED " + reference.getDisplayName()
                    + " " + reference.getDetectedFormat());
            OperationResult<ImportPreview> result = appContainer.importSessionService.previewImport(
                    new AndroidDocumentSource(getContentResolver(), reference));
            if (result.isSuccess()) {
                currentImportPreview = result.getValue();
                importError = null;
                appContainer.diagnosticService.logInfo("Import", "IMPORT_VALIDATION_COMPLETED rows="
                        + currentImportPreview.getValidationResult().getAcceptedRows().size()
                        + " fatal=" + currentImportPreview.getValidationResult().getFatalCount()
                        + " warning=" + currentImportPreview.getValidationResult().getWarningCount());
            } else {
                currentImportPreview = null;
                importError = result.getMessage();
                appContainer.diagnosticService.logWarning("Import", "IMPORT_PARSE_FAILED " + result.getMessage());
            }
            importReading = false;
            appContainer.executors.mainThread(this::renderCurrentScreen);
        });
    }

    private DocumentReference documentReferenceFor(Uri uri) {
        String displayName = uri.getLastPathSegment();
        long size = -1L;
        try (Cursor cursor = getContentResolver().query(uri, null, null, null, null)) {
            if (cursor != null && cursor.moveToFirst()) {
                int nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                int sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE);
                if (nameIndex >= 0) {
                    displayName = cursor.getString(nameIndex);
                }
                if (sizeIndex >= 0 && !cursor.isNull(sizeIndex)) {
                    size = cursor.getLong(sizeIndex);
                }
            }
        }
        String mimeType = getContentResolver().getType(uri);
        AssetFileFormat format = AssetFileFormatDetector.detect(displayName, mimeType);
        return new DocumentReference(uri, displayName, mimeType, size, format);
    }

    private void createSessionFromPreview(boolean warningsAccepted) {
        importCreating = true;
        renderCurrentScreen();
        appContainer.executors.diskIO().execute(() -> {
            OperationResult<CreatedAuditSession> result = appContainer.importSessionService.createSessionFromPreview(
                    currentImportPreview,
                    new ImportConfirmation(warningsAccepted));
            if (result.isSuccess()) {
                createdAuditSession = result.getValue();
                activeSession = appContainer.auditSessionService.getActiveSession();
                currentImportPreview = null;
                importError = null;
                appContainer.diagnosticService.logInfo("Import", "IMPORT_SESSION_CREATED assets="
                        + createdAuditSession.getAssetCount());
            } else {
                importError = result.getMessage();
                appContainer.diagnosticService.logError("Import", "IMPORT_TRANSACTION_FAILED " + result.getMessage(), null);
            }
            importCreating = false;
            recentDiagnostics = appContainer.diagnosticService.listRecent(8);
            appContainer.executors.mainThread(this::renderCurrentScreen);
        });
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
