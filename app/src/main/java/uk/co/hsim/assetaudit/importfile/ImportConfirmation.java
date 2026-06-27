package uk.co.hsim.assetaudit.importfile;

public final class ImportConfirmation {
    private final boolean warningsAccepted;

    public ImportConfirmation(boolean warningsAccepted) {
        this.warningsAccepted = warningsAccepted;
    }

    public boolean isWarningsAccepted() {
        return warningsAccepted;
    }
}
