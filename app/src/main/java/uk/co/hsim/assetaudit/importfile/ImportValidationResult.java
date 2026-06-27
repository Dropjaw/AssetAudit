package uk.co.hsim.assetaudit.importfile;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class ImportValidationResult {
    private final boolean importable;
    private final int fatalCount;
    private final int warningCount;
    private final int infoCount;
    private final List<ImportIssue> issues;
    private final List<AssetImportRow> acceptedRows;

    public ImportValidationResult(boolean importable, int fatalCount, int warningCount, int infoCount,
                                  List<ImportIssue> issues, List<AssetImportRow> acceptedRows) {
        this.importable = importable;
        this.fatalCount = fatalCount;
        this.warningCount = warningCount;
        this.infoCount = infoCount;
        this.issues = Collections.unmodifiableList(new ArrayList<>(issues));
        this.acceptedRows = Collections.unmodifiableList(new ArrayList<>(acceptedRows));
    }

    public boolean isImportable() {
        return importable;
    }

    public int getFatalCount() {
        return fatalCount;
    }

    public int getWarningCount() {
        return warningCount;
    }

    public int getInfoCount() {
        return infoCount;
    }

    public List<ImportIssue> getIssues() {
        return issues;
    }

    public List<AssetImportRow> getAcceptedRows() {
        return acceptedRows;
    }
}
