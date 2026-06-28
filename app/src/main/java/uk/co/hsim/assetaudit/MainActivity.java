package uk.co.hsim.assetaudit;

import android.content.Context;
import android.content.IntentFilter;
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
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import uk.co.hsim.assetaudit.app.AppContainer;
import uk.co.hsim.assetaudit.data.db.AuditDatabase;
import uk.co.hsim.assetaudit.data.entity.AssetEntity;
import uk.co.hsim.assetaudit.data.entity.AuditSessionEntity;
import uk.co.hsim.assetaudit.data.entity.DiagnosticLogEntity;
import uk.co.hsim.assetaudit.domain.results.OperationResult;
import uk.co.hsim.assetaudit.domain.enums.ScanResultType;
import uk.co.hsim.assetaudit.importfile.AndroidDocumentSource;
import uk.co.hsim.assetaudit.importfile.AssetFileFormat;
import uk.co.hsim.assetaudit.importfile.AssetFileFormatDetector;
import uk.co.hsim.assetaudit.importfile.CreatedAuditSession;
import uk.co.hsim.assetaudit.importfile.DocumentReference;
import uk.co.hsim.assetaudit.importfile.ImportConfirmation;
import uk.co.hsim.assetaudit.importfile.ImportIssue;
import uk.co.hsim.assetaudit.importfile.ImportPreview;
import uk.co.hsim.assetaudit.scanner.DataWedgeConstants;
import uk.co.hsim.assetaudit.scanner.DataWedgeProfileConfig;
import uk.co.hsim.assetaudit.scanner.DataWedgeScanReceiver;
import uk.co.hsim.assetaudit.scanner.ScannerPayload;
import uk.co.hsim.assetaudit.scanner.ScannerRouteResult;
import uk.co.hsim.assetaudit.service.DepartmentAuditContext;
import uk.co.hsim.assetaudit.service.DepartmentDashboardRow;
import uk.co.hsim.assetaudit.service.DuplicateReviewState;
import uk.co.hsim.assetaudit.service.ExceptionResolutionResult;
import uk.co.hsim.assetaudit.service.FinishDepartmentPreview;
import uk.co.hsim.assetaudit.service.MovementConfirmationRequest;
import uk.co.hsim.assetaudit.service.NewAssetDraft;
import uk.co.hsim.assetaudit.service.ScanProcessingResult;
import uk.co.hsim.assetaudit.service.ScanRequest;
import uk.co.hsim.assetaudit.service.SettingsKeys;
import uk.co.hsim.assetaudit.service.SkipAssetsRequest;
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
    private String selectedDepartmentName;
    private DepartmentAuditContext currentDepartmentContext;
    private ScanProcessingResult lastScanResult;
    private boolean departmentLoading;
    private boolean scanProcessing;
    private EditText manualBarcodeInput;
    private DataWedgeScanReceiver dataWedgeScanReceiver;
    private boolean scannerReceiverRegistered;
    private boolean liveScannerEnabled = true;
    private boolean scannerDiagnosticsEnabled = true;
    private String dataWedgeProfileName = DataWedgeConstants.PROFILE_NAME;
    private String dataWedgeIntentAction = DataWedgeConstants.ACTION_SCAN;
    private String dataWedgeIntentCategory = DataWedgeConstants.CATEGORY_SCAN;
    private String lastHardwareScanSummary = "No hardware scan received.";
    private String lastProfileSetupResult = "Profile setup has not been requested.";
    private boolean exceptionActionInProgress;
    private String exceptionError;
    private boolean newAssetFormVisible;
    private FinishDepartmentPreview finishDepartmentPreview;
    private DuplicateReviewState duplicateReviewState;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        appContainer = AppContainer.get(this);
        dataWedgeScanReceiver = new DataWedgeScanReceiver(appContainer.scannerPayloadParser, appContainer.clock);
        dataWedgeScanReceiver.setListener(new DataWedgeScanReceiver.Listener() {
            @Override
            public void onScannerPayload(ScannerPayload payload) {
                handleScannerPayload(payload);
            }

            @Override
            public void onScannerPayloadIgnored(String reason) {
                lastHardwareScanSummary = "Ignored scanner payload: " + reason;
                if (scannerDiagnosticsEnabled) {
                    appContainer.executors.diskIO().execute(() ->
                            appContainer.diagnosticService.logWarning("Scanner", "PAYLOAD_IGNORED " + reason));
                }
                renderCurrentScreen();
            }
        });
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
            loadScannerSettingsOnDisk();
            recentDiagnostics = appContainer.diagnosticService.listRecent(8);
            appContainer.executors.mainThread(this::renderCurrentScreen);
        });
    }

    @Override
    protected void onPause() {
        unregisterScannerReceiver();
        super.onPause();
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
                ? "No active audit session. Import a CSV file to begin."
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
                renderAuditScan();
                break;
            case REPORTS:
                renderPlaceholder(
                        "Reports and export",
                        "Updated asset files, summaries, and exception reports will be generated after import and audit workflows exist.",
                        "Export buttons are disabled until a later phase."
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
        updateScannerReceiverRegistration();
    }

    private void renderHome() {
        addBody("Asset Audit is ready for local department auditing. Import a CSV file, open a department, and record expected manual scans.");
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
        addBody("Open a department to audit its remaining expected assets with manual barcode entry.");
        appContainer.executors.diskIO().execute(() -> {
            List<DepartmentDashboardRow> departments = appContainer.departmentSummaryService
                    .getDashboardRows(activeSession.sessionId);
            appContainer.executors.mainThread(() -> {
                content.removeAllViews();
                addSectionHeading("Department dashboard");
                if (departments.isEmpty()) {
                    addBody("No department summaries exist for the active session.");
                } else {
                    for (DepartmentDashboardRow department : departments) {
                        addKeyValue(department.getDepartmentName(),
                                department.getScannedCount() + " scanned / "
                                        + department.getExpectedCount() + " expected, "
                                        + department.getRemainingCount() + " remaining, "
                                        + department.getStatus());
                        Button open = button("Open " + department.getDepartmentName());
                        open.setOnClickListener(v -> openDepartmentAudit(department.getDepartmentName()));
                        content.addView(open, matchWrap());
                    }
                }
                addBackButton();
            });
        });
    }

    private void openDepartmentAudit(String departmentName) {
        selectedDepartmentName = departmentName;
        currentDepartmentContext = null;
        lastScanResult = null;
        departmentLoading = false;
        scanProcessing = false;
        navigator.navigateTo(Screen.AUDIT_SCAN);
        renderCurrentScreen();
    }

    private void renderAuditScan() {
        if (activeSession == null) {
            renderPlaceholder(
                    "Audit scan",
                    "Manual department scanning is available after an import creates an active local session.",
                    "No imported session is available yet."
            );
            return;
        }
        if (selectedDepartmentName == null || selectedDepartmentName.trim().isEmpty()) {
            addSectionHeading("Choose department");
            addBody("Open a department before scanning so each asset is checked against the expected list.");
            Button departments = button("Open Departments");
            departments.setOnClickListener(v -> {
                navigator.navigateTo(Screen.DEPARTMENTS);
                renderCurrentScreen();
            });
            content.addView(departments, matchWrap());
            addBackButton();
            return;
        }
        if (currentDepartmentContext == null && !departmentLoading) {
            loadDepartmentContext();
        }
        addSectionHeading(selectedDepartmentName);
        if (departmentLoading) {
            addBody("Loading remaining assets...");
            addBackButton();
            return;
        }
        if (currentDepartmentContext == null) {
            addBody("Department details are not available yet.");
            addBackButton();
            return;
        }

        DepartmentDashboardRow progress = currentDepartmentContext.getProgress();
        addKeyValue("Expected", String.valueOf(progress.getExpectedCount()));
        addKeyValue("Scanned", String.valueOf(progress.getScannedCount()));
        addKeyValue("Remaining", String.valueOf(progress.getRemainingCount()));
        addKeyValue("Status", progress.getStatus().name());

        renderScannerStatus();
        if (exceptionError != null) {
            addSectionHeading("Exception action failed");
            addBody(exceptionError);
        }
        renderExceptionPanels();

        addSectionHeading("Manual entry");
        manualBarcodeInput = editText("Asset tag ID");
        content.addView(manualBarcodeInput, matchWrap());

        Button scan = button(scanProcessing ? "Processing..." : "Process Scan");
        scan.setEnabled(!scanProcessing && !exceptionActionInProgress);
        scan.setOnClickListener(v -> processManualScan(manualBarcodeInput.getText().toString()));
        content.addView(scan, matchWrap());

        Button clearResult = button("Clear Result");
        clearResult.setEnabled(lastScanResult != null && !scanProcessing);
        clearResult.setOnClickListener(v -> {
            lastScanResult = null;
            renderCurrentScreen();
        });
        content.addView(clearResult, matchWrap());

        if (scanProcessing) {
            addBody("Checking asset against this department...");
        }
        if (lastScanResult != null) {
            renderScanResult(lastScanResult);
        }

        Button finish = button("Finish Department");
        finish.setEnabled(!exceptionActionInProgress);
        finish.setOnClickListener(v -> loadFinishDepartmentPreview());
        content.addView(finish, matchWrap());
        if (finishDepartmentPreview != null) {
            renderFinishDepartmentPreview();
        }

        addSectionHeading("Remaining assets");
        List<AssetEntity> remainingAssets = currentDepartmentContext.getRemainingAssets();
        if (remainingAssets.isEmpty()) {
            addBody("No remaining expected assets in this department.");
        } else {
            int limit = Math.min(remainingAssets.size(), 40);
            for (int i = 0; i < limit; i++) {
                AssetEntity asset = remainingAssets.get(i);
                addBody(asset.assetTagId + " - " + asset.description);
            }
            if (remainingAssets.size() > limit) {
                addBody("Additional remaining assets: " + (remainingAssets.size() - limit));
            }
        }
        addBackButton();
    }

    private void loadDepartmentContext() {
        departmentLoading = true;
        appContainer.executors.diskIO().execute(() -> {
            DepartmentAuditContext context = appContainer.departmentSummaryService.getDepartmentAuditContext(
                    activeSession.sessionId,
                    activeSession.auditName,
                    selectedDepartmentName
            );
            appContainer.executors.mainThread(() -> {
                currentDepartmentContext = context;
                departmentLoading = false;
                renderCurrentScreen();
            });
        });
    }

    private void processManualScan(String barcodeRaw) {
        if (scanProcessing || exceptionActionInProgress || activeSession == null || selectedDepartmentName == null) {
            return;
        }
        scanProcessing = true;
        renderCurrentScreen();
        appContainer.executors.diskIO().execute(() -> {
            ScanProcessingResult result = appContainer.scanProcessor.processScan(new ScanRequest(
                    activeSession.sessionId,
                    selectedDepartmentName,
                    barcodeRaw,
                    "MANUAL"
            ));
            DepartmentAuditContext context = appContainer.departmentSummaryService.getDepartmentAuditContext(
                    activeSession.sessionId,
                    activeSession.auditName,
                    selectedDepartmentName
            );
            appContainer.executors.mainThread(() -> {
                lastScanResult = result;
                currentDepartmentContext = context;
                scanProcessing = false;
                refreshExceptionStateAfterScan(result);
                renderCurrentScreen();
            });
        });
    }

    private void handleScannerPayload(ScannerPayload payload) {
        if (exceptionActionInProgress) {
            lastHardwareScanSummary = "Scanner ignored during exception action.";
            renderCurrentScreen();
            return;
        }
        lastHardwareScanSummary = "Received " + safeScannerTag(payload.getData())
                + " (" + payload.getSymbology() + ", " + payload.getSource() + ")";
        appContainer.scannerEventRouter.route(payload, new uk.co.hsim.assetaudit.scanner.ScannerEventRouter.ContextProvider() {
            @Override
            public AuditSessionEntity getActiveSession() {
                return activeSession;
            }

            @Override
            public String getSelectedDepartment() {
                return selectedDepartmentName;
            }

            @Override
            public boolean isAuditScanVisible() {
                return navigator.getCurrent() == Screen.AUDIT_SCAN;
            }

            @Override
            public boolean isLiveScannerEnabled() {
                return liveScannerEnabled && !exceptionActionInProgress;
            }
        }, this::handleScannerRouteResult);
    }

    private void handleScannerRouteResult(ScannerRouteResult result) {
        if (result.isProcessed()) {
            lastScanResult = result.getScanResult();
            currentDepartmentContext = result.getDepartmentContext();
            lastHardwareScanSummary = "Processed " + safeScannerTag(result.getPayload().getData())
                    + ": " + result.getScanResult().getResultType();
            refreshExceptionStateAfterScan(result.getScanResult());
        } else {
            lastHardwareScanSummary = result.getMessage();
        }
        renderCurrentScreen();
    }

    private void renderScannerStatus() {
        addSectionHeading("Zebra scanner");
        addKeyValue("Live scanner", liveScannerEnabled ? "Enabled" : "Disabled");
        addKeyValue("Receiver", scannerReceiverRegistered ? "Listening" : scannerReadinessMessage());
        addKeyValue("Profile", dataWedgeProfileName);
        addKeyValue("Intent action", dataWedgeIntentAction);
        addKeyValue("Last hardware scan", lastHardwareScanSummary);
        addKeyValue("Profile setup", lastProfileSetupResult);

        Button configure = button("Configure DataWedge Profile");
        configure.setOnClickListener(v -> configureDataWedgeProfile());
        content.addView(configure, matchWrap());

        addBody("Manual TC21 fallback: create profile AssetAudit_TC21, associate package uk.co.hsim.assetaudit, enable Barcode input, disable Keystroke output, enable Intent output, set action uk.co.hsim.assetaudit.SCAN, category android.intent.category.DEFAULT, and delivery Broadcast Intent.");
    }

    private String scannerReadinessMessage() {
        if (!liveScannerEnabled) {
            return "Disabled in Settings";
        }
        if (activeSession == null) {
            return "Waiting for active session";
        }
        if (selectedDepartmentName == null || selectedDepartmentName.trim().isEmpty()) {
            return "Waiting for selected department";
        }
        if (navigator.getCurrent() != Screen.AUDIT_SCAN) {
            return "Inactive outside Audit Scan";
        }
        return "Pending registration";
    }

    private void configureDataWedgeProfile() {
        DataWedgeProfileConfig config = new DataWedgeProfileConfig(
                dataWedgeProfileName,
                getPackageName(),
                dataWedgeIntentAction,
                dataWedgeIntentCategory
        );
        lastProfileSetupResult = appContainer.dataWedgeProfileManager.configureProfile(config);
        lastHardwareScanSummary = "Waiting for DataWedge scan intent.";
        appContainer.executors.diskIO().execute(() -> {
            appContainer.diagnosticService.logInfo("Scanner", "DATAWEDGE_PROFILE_REQUESTED "
                    + dataWedgeProfileName + " " + dataWedgeIntentAction);
            recentDiagnostics = appContainer.diagnosticService.listRecent(8);
        });
        renderCurrentScreen();
    }

    private void updateScannerReceiverRegistration() {
        boolean shouldRegister = liveScannerEnabled
                && !exceptionActionInProgress
                && navigator.getCurrent() == Screen.AUDIT_SCAN
                && activeSession != null
                && selectedDepartmentName != null
                && !selectedDepartmentName.trim().isEmpty();
        if (shouldRegister && !scannerReceiverRegistered) {
            IntentFilter filter = new IntentFilter(dataWedgeIntentAction);
            if (dataWedgeIntentCategory != null && !dataWedgeIntentCategory.trim().isEmpty()) {
                filter.addCategory(dataWedgeIntentCategory);
            }
            registerReceiver(dataWedgeScanReceiver, filter, Context.RECEIVER_EXPORTED);
            scannerReceiverRegistered = true;
            if (scannerDiagnosticsEnabled) {
                appContainer.executors.diskIO().execute(() ->
                        appContainer.diagnosticService.logInfo("Scanner", "RECEIVER_REGISTERED " + dataWedgeIntentAction));
            }
            return;
        }
        if (!shouldRegister && scannerReceiverRegistered) {
            unregisterScannerReceiver();
        }
    }

    private void unregisterScannerReceiver() {
        if (!scannerReceiverRegistered) {
            return;
        }
        unregisterReceiver(dataWedgeScanReceiver);
        scannerReceiverRegistered = false;
        if (scannerDiagnosticsEnabled) {
            appContainer.executors.diskIO().execute(() ->
                    appContainer.diagnosticService.logInfo("Scanner", "RECEIVER_UNREGISTERED"));
        }
    }

    private void loadScannerSettingsOnDisk() {
        liveScannerEnabled = appContainer.settingsService.getBooleanSetting(SettingsKeys.LIVE_SCANNER_ENABLED, true);
        scannerDiagnosticsEnabled = appContainer.settingsService.getBooleanSetting(SettingsKeys.SCANNER_DIAGNOSTICS_ENABLED, true);
        dataWedgeProfileName = appContainer.settingsService.getStringSetting(SettingsKeys.DATAWEDGE_PROFILE_NAME, DataWedgeConstants.PROFILE_NAME);
        dataWedgeIntentAction = appContainer.settingsService.getStringSetting(SettingsKeys.DATAWEDGE_INTENT_ACTION, DataWedgeConstants.ACTION_SCAN);
        dataWedgeIntentCategory = appContainer.settingsService.getStringSetting(SettingsKeys.DATAWEDGE_INTENT_CATEGORY, DataWedgeConstants.CATEGORY_SCAN);
    }

    private String safeScannerTag(String value) {
        if (value == null) {
            return "";
        }
        return value.length() <= 16 ? value : value.substring(0, 16) + "...";
    }

    private void refreshExceptionStateAfterScan(ScanProcessingResult result) {
        exceptionError = null;
        finishDepartmentPreview = null;
        newAssetFormVisible = false;
        duplicateReviewState = null;
        if (result.getResultType() == ScanResultType.DUPLICATE_SCAN && activeSession != null) {
            appContainer.executors.diskIO().execute(() -> {
                OperationResult<DuplicateReviewState> review = appContainer.exceptionResolutionService
                        .buildDuplicateReview(activeSession.sessionId, result.getAssetTagId());
                if (review.isSuccess()) {
                    duplicateReviewState = review.getValue();
                    appContainer.executors.mainThread(this::renderCurrentScreen);
                }
            });
        }
    }

    private void renderExceptionPanels() {
        if (lastScanResult == null) {
            return;
        }
        if (lastScanResult.getResultType() == ScanResultType.FOUND_IN_OTHER_DEPARTMENT_REQUIRES_CONFIRMATION) {
            renderMovementPanel();
        } else if (lastScanResult.getResultType() == ScanResultType.UNKNOWN_ASSET_REQUIRES_INPUT) {
            renderNewAssetPanel();
        } else if (lastScanResult.getResultType() == ScanResultType.DUPLICATE_SCAN) {
            renderDuplicatePanel();
        }
    }

    private void renderMovementPanel() {
        addSectionHeading("Movement confirmation");
        addKeyValue("Asset tag", lastScanResult.getAssetTagId());
        addKeyValue("Current department", lastScanResult.getAssetDepartment());
        addKeyValue("Selected department", selectedDepartmentName);
        addBody("Confirm only if the asset is physically present in this department.");

        Button confirm = button(exceptionActionInProgress ? "Confirming..." : "Confirm Movement");
        confirm.setEnabled(!exceptionActionInProgress);
        confirm.setOnClickListener(v -> confirmMovement());
        content.addView(confirm, matchWrap());

        Button cancel = button("Cancel Movement");
        cancel.setEnabled(!exceptionActionInProgress);
        cancel.setOnClickListener(v -> {
            lastScanResult = null;
            exceptionError = null;
            renderCurrentScreen();
        });
        content.addView(cancel, matchWrap());
    }

    private void confirmMovement() {
        if (activeSession == null || lastScanResult == null) {
            return;
        }
        exceptionActionInProgress = true;
        renderCurrentScreen();
        MovementConfirmationRequest request = new MovementConfirmationRequest(
                activeSession.sessionId,
                lastScanResult.getAssetTagId(),
                selectedDepartmentName,
                lastScanResult.getAssetDepartment(),
                "Confirmed from audit scan"
        );
        appContainer.executors.diskIO().execute(() -> {
            OperationResult<ExceptionResolutionResult> result = appContainer.exceptionResolutionService.confirmMovement(request);
            handleExceptionResult(result);
        });
    }

    private void renderNewAssetPanel() {
        addSectionHeading("Unknown asset");
        addKeyValue("Asset tag", lastScanResult.getAssetTagId());
        if (!newAssetFormVisible) {
            Button add = button("Add New Asset");
            add.setEnabled(!exceptionActionInProgress);
            add.setOnClickListener(v -> {
                newAssetFormVisible = true;
                renderCurrentScreen();
            });
            content.addView(add, matchWrap());
            return;
        }

        EditText tag = editText("Asset Tag ID");
        tag.setText(lastScanResult.getAssetTagId());
        EditText department = editText("Department");
        department.setText(selectedDepartmentName);
        EditText description = editText("Description");
        EditText status = editText("Status");
        status.setText("Found during audit");
        EditText site = editText("Site");
        EditText location = editText("Location");
        EditText category = editText("Category");
        EditText subCategory = editText("Sub Category");
        EditText owner = editText("Owner");
        EditText primaryUser = editText("Primary User");
        EditText notes = editText("Notes");

        content.addView(tag, matchWrap());
        content.addView(department, matchWrap());
        content.addView(description, matchWrap());
        content.addView(status, matchWrap());
        content.addView(site, matchWrap());
        content.addView(location, matchWrap());
        content.addView(category, matchWrap());
        content.addView(subCategory, matchWrap());
        content.addView(owner, matchWrap());
        content.addView(primaryUser, matchWrap());
        content.addView(notes, matchWrap());

        Button create = button(exceptionActionInProgress ? "Adding..." : "Add Asset");
        create.setEnabled(!exceptionActionInProgress);
        create.setOnClickListener(v -> createNewAsset(new NewAssetDraft(
                activeSession.sessionId,
                tag.getText().toString(),
                department.getText().toString(),
                description.getText().toString(),
                status.getText().toString(),
                site.getText().toString(),
                location.getText().toString(),
                category.getText().toString(),
                subCategory.getText().toString(),
                owner.getText().toString(),
                primaryUser.getText().toString(),
                notes.getText().toString()
        )));
        content.addView(create, matchWrap());

        Button cancel = button("Cancel New Asset");
        cancel.setEnabled(!exceptionActionInProgress);
        cancel.setOnClickListener(v -> {
            newAssetFormVisible = false;
            renderCurrentScreen();
        });
        content.addView(cancel, matchWrap());
    }

    private void createNewAsset(NewAssetDraft draft) {
        exceptionActionInProgress = true;
        renderCurrentScreen();
        appContainer.executors.diskIO().execute(() -> {
            boolean allowed = appContainer.settingsService.getBooleanSetting(SettingsKeys.ALLOW_NEW_ASSET_CREATION, true);
            OperationResult<ExceptionResolutionResult> result = allowed
                    ? appContainer.exceptionResolutionService.createNewAsset(draft)
                    : OperationResult.fail(uk.co.hsim.assetaudit.domain.results.ErrorCode.FEATURE_NOT_AVAILABLE,
                    "New asset creation is disabled in Settings.");
            handleExceptionResult(result);
        });
    }

    private void renderDuplicatePanel() {
        addSectionHeading("Duplicate review");
        if (duplicateReviewState == null) {
            addBody("Loading duplicate context...");
            return;
        }
        AssetEntity asset = duplicateReviewState.getAsset();
        addKeyValue("Asset tag", asset.assetTagId);
        addKeyValue("Department", asset.department == null ? "" : asset.department);
        addKeyValue("Audit status", asset.auditStatus.name());
        addKeyValue("Recent events", String.valueOf(duplicateReviewState.getRecentEvents().size()));
        addBody("Duplicate scans are review evidence only. No counts or asset status are changed.");
    }

    private void loadFinishDepartmentPreview() {
        if (activeSession == null || selectedDepartmentName == null) {
            return;
        }
        exceptionActionInProgress = true;
        renderCurrentScreen();
        appContainer.executors.diskIO().execute(() -> {
            OperationResult<FinishDepartmentPreview> result = appContainer.exceptionResolutionService
                    .previewFinishDepartment(activeSession.sessionId, selectedDepartmentName);
            appContainer.executors.mainThread(() -> {
                exceptionActionInProgress = false;
                if (result.isSuccess()) {
                    finishDepartmentPreview = result.getValue();
                    exceptionError = null;
                } else {
                    exceptionError = result.getMessage();
                }
                renderCurrentScreen();
            });
        });
    }

    private void renderFinishDepartmentPreview() {
        addSectionHeading("Finish department");
        addKeyValue("Remaining", String.valueOf(finishDepartmentPreview.getRemainingCount()));
        addKeyValue("Moved", String.valueOf(finishDepartmentPreview.getExceptionCounts().getMovedCount()));
        addKeyValue("New", String.valueOf(finishDepartmentPreview.getExceptionCounts().getNewAssetCount()));
        addKeyValue("Missing", String.valueOf(finishDepartmentPreview.getExceptionCounts().getMissingCount()));
        addKeyValue("Skipped", String.valueOf(finishDepartmentPreview.getExceptionCounts().getSkippedCount()));
        if (finishDepartmentPreview.getRemainingAssets().isEmpty()) {
            addBody("No remaining assets. Department can be completed from current audit state.");
            return;
        }
        int shown = Math.min(10, finishDepartmentPreview.getRemainingAssets().size());
        for (int i = 0; i < shown; i++) {
            AssetEntity asset = finishDepartmentPreview.getRemainingAssets().get(i);
            addBody(asset.assetTagId + " - " + asset.description);
        }
        if (finishDepartmentPreview.getRemainingAssets().size() > shown) {
            addBody("Additional remaining assets: " + (finishDepartmentPreview.getRemainingAssets().size() - shown));
        }

        Button missing = button("Mark All Remaining Missing");
        missing.setEnabled(!exceptionActionInProgress);
        missing.setOnClickListener(v -> markAllRemainingMissing());
        content.addView(missing, matchWrap());

        EditText skipReason = editText("Skip reason");
        content.addView(skipReason, matchWrap());
        Button skip = button("Skip All Remaining With Reason");
        skip.setEnabled(!exceptionActionInProgress);
        skip.setOnClickListener(v -> skipAllRemaining(skipReason.getText().toString()));
        content.addView(skip, matchWrap());

        Button close = button("Return to Scanning");
        close.setOnClickListener(v -> {
            finishDepartmentPreview = null;
            renderCurrentScreen();
        });
        content.addView(close, matchWrap());
    }

    private void markAllRemainingMissing() {
        List<String> tags = tagsFromFinishPreview();
        exceptionActionInProgress = true;
        renderCurrentScreen();
        appContainer.executors.diskIO().execute(() ->
                handleExceptionResult(appContainer.exceptionResolutionService.markRemainingMissing(
                        activeSession.sessionId, selectedDepartmentName, tags)));
    }

    private void skipAllRemaining(String reason) {
        List<String> tags = tagsFromFinishPreview();
        exceptionActionInProgress = true;
        renderCurrentScreen();
        appContainer.executors.diskIO().execute(() ->
                handleExceptionResult(appContainer.exceptionResolutionService.skipAssets(
                        new SkipAssetsRequest(activeSession.sessionId, selectedDepartmentName, tags, reason))));
    }

    private List<String> tagsFromFinishPreview() {
        List<String> tags = new ArrayList<>();
        if (finishDepartmentPreview == null) {
            return tags;
        }
        for (AssetEntity asset : finishDepartmentPreview.getRemainingAssets()) {
            tags.add(asset.assetTagId);
        }
        return tags;
    }

    private void handleExceptionResult(OperationResult<ExceptionResolutionResult> result) {
        DepartmentAuditContext refreshed = activeSession == null || selectedDepartmentName == null
                ? null
                : appContainer.departmentSummaryService.getDepartmentAuditContext(
                activeSession.sessionId, activeSession.auditName, selectedDepartmentName);
        recentDiagnostics = appContainer.diagnosticService.listRecent(8);
        appContainer.executors.mainThread(() -> {
            exceptionActionInProgress = false;
            if (result.isSuccess()) {
                currentDepartmentContext = result.getValue().getDepartmentContext() == null
                        ? refreshed
                        : result.getValue().getDepartmentContext();
                lastScanResult = null;
                exceptionError = null;
                newAssetFormVisible = false;
                duplicateReviewState = null;
                finishDepartmentPreview = null;
                Toast.makeText(this, result.getValue().getMessage(), Toast.LENGTH_SHORT).show();
            } else {
                currentDepartmentContext = refreshed;
                exceptionError = result.getMessage();
            }
            renderCurrentScreen();
        });
    }

    private void renderScanResult(ScanProcessingResult result) {
        addSectionHeading(result.isSuccess() ? "Scan accepted" : "Scan not accepted");
        addKeyValue("Result", result.getResultType().name());
        if (result.getAssetTagId() != null && !result.getAssetTagId().isEmpty()) {
            addKeyValue("Asset tag", result.getAssetTagId());
        }
        if (result.getDescription() != null && !result.getDescription().isEmpty()) {
            addKeyValue("Description", result.getDescription());
        }
        if (result.getAssetDepartment() != null && !result.getAssetDepartment().isEmpty()) {
            addKeyValue("Asset department", result.getAssetDepartment());
        }
        addBody(result.getMessage());
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
        CheckBox liveScanner = checkbox("Enable Zebra DataWedge scanner");
        CheckBox scannerDiagnostics = checkbox("Enable scanner diagnostics");
        EditText unassigned = editText("Unassigned department label");
        EditText exportFormat = editText("Default export format");
        EditText profileName = editText("DataWedge profile name");
        EditText intentAction = editText("DataWedge intent action");
        EditText intentCategory = editText("DataWedge intent category");

        content.addView(movement);
        content.addView(manual);
        content.addView(newAsset);
        content.addView(diagnostics);
        content.addView(liveScanner);
        content.addView(scannerDiagnostics);
        content.addView(unassigned, matchWrap());
        content.addView(exportFormat, matchWrap());
        content.addView(profileName, matchWrap());
        content.addView(intentAction, matchWrap());
        content.addView(intentCategory, matchWrap());

        appContainer.executors.diskIO().execute(() -> {
            boolean movementValue = appContainer.settingsService.getBooleanSetting(SettingsKeys.REQUIRE_MOVEMENT_CONFIRMATION, true);
            boolean manualValue = appContainer.settingsService.getBooleanSetting(SettingsKeys.ALLOW_MANUAL_BARCODE_ENTRY, true);
            boolean newAssetValue = appContainer.settingsService.getBooleanSetting(SettingsKeys.ALLOW_NEW_ASSET_CREATION, true);
            boolean diagnosticsValue = appContainer.settingsService.getBooleanSetting(SettingsKeys.DIAGNOSTIC_LOGGING_ENABLED, true);
            boolean liveScannerValue = appContainer.settingsService.getBooleanSetting(SettingsKeys.LIVE_SCANNER_ENABLED, true);
            boolean scannerDiagnosticsValue = appContainer.settingsService.getBooleanSetting(SettingsKeys.SCANNER_DIAGNOSTICS_ENABLED, true);
            String unassignedValue = appContainer.settingsService.getStringSetting(SettingsKeys.UNASSIGNED_DEPARTMENT_LABEL, "Unassigned / Blank Department");
            String exportValue = appContainer.settingsService.getStringSetting(SettingsKeys.DEFAULT_EXPORT_FORMAT, "CSV");
            String profileNameValue = appContainer.settingsService.getStringSetting(SettingsKeys.DATAWEDGE_PROFILE_NAME, DataWedgeConstants.PROFILE_NAME);
            String intentActionValue = appContainer.settingsService.getStringSetting(SettingsKeys.DATAWEDGE_INTENT_ACTION, DataWedgeConstants.ACTION_SCAN);
            String intentCategoryValue = appContainer.settingsService.getStringSetting(SettingsKeys.DATAWEDGE_INTENT_CATEGORY, DataWedgeConstants.CATEGORY_SCAN);
            appContainer.executors.mainThread(() -> {
                movement.setChecked(movementValue);
                manual.setChecked(manualValue);
                newAsset.setChecked(newAssetValue);
                diagnostics.setChecked(diagnosticsValue);
                liveScanner.setChecked(liveScannerValue);
                scannerDiagnostics.setChecked(scannerDiagnosticsValue);
                unassigned.setText(unassignedValue);
                exportFormat.setText(exportValue);
                profileName.setText(profileNameValue);
                intentAction.setText(intentActionValue);
                intentCategory.setText(intentCategoryValue);
            });
        });

        Button save = button("Save settings");
        save.setOnClickListener(v -> {
            boolean movementValue = movement.isChecked();
            boolean manualValue = manual.isChecked();
            boolean newAssetValue = newAsset.isChecked();
            boolean diagnosticsValue = diagnostics.isChecked();
            boolean liveScannerValue = liveScanner.isChecked();
            boolean scannerDiagnosticsValue = scannerDiagnostics.isChecked();
            String unassignedValue = unassigned.getText().toString();
            String exportValue = exportFormat.getText().toString();
            String profileNameValue = profileName.getText().toString();
            String intentActionValue = intentAction.getText().toString();
            String intentCategoryValue = intentCategory.getText().toString();
            appContainer.executors.diskIO().execute(() -> {
            appContainer.settingsService.setBooleanSetting(SettingsKeys.REQUIRE_MOVEMENT_CONFIRMATION, movementValue);
            appContainer.settingsService.setBooleanSetting(SettingsKeys.ALLOW_MANUAL_BARCODE_ENTRY, manualValue);
            appContainer.settingsService.setBooleanSetting(SettingsKeys.ALLOW_NEW_ASSET_CREATION, newAssetValue);
            appContainer.settingsService.setBooleanSetting(SettingsKeys.DIAGNOSTIC_LOGGING_ENABLED, diagnosticsValue);
            appContainer.settingsService.setBooleanSetting(SettingsKeys.LIVE_SCANNER_ENABLED, liveScannerValue);
            appContainer.settingsService.setBooleanSetting(SettingsKeys.SCANNER_DIAGNOSTICS_ENABLED, scannerDiagnosticsValue);
            appContainer.settingsService.setStringSetting(SettingsKeys.UNASSIGNED_DEPARTMENT_LABEL, unassignedValue);
            appContainer.settingsService.setStringSetting(SettingsKeys.DEFAULT_EXPORT_FORMAT, exportValue);
            appContainer.settingsService.setStringSetting(SettingsKeys.DATAWEDGE_PROFILE_NAME, profileNameValue);
            appContainer.settingsService.setStringSetting(SettingsKeys.DATAWEDGE_INTENT_ACTION, intentActionValue);
            appContainer.settingsService.setStringSetting(SettingsKeys.DATAWEDGE_INTENT_CATEGORY, intentCategoryValue);
            loadScannerSettingsOnDisk();
            appContainer.diagnosticService.logInfo("Settings", "Foundation settings saved");
            recentDiagnostics = appContainer.diagnosticService.listRecent(8);
            appContainer.executors.mainThread(() -> {
                unregisterScannerReceiver();
                updateScannerReceiverRegistration();
                Toast.makeText(this, "Settings saved", Toast.LENGTH_SHORT).show();
            });
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
