package uk.co.hsim.assetaudit.service;

public final class ExceptionCountSummary {
    private final int movedCount;
    private final int newAssetCount;
    private final int missingCount;
    private final int skippedCount;
    private final int duplicateScanCount;
    private final int invalidScanCount;

    public ExceptionCountSummary(int movedCount, int newAssetCount, int missingCount, int skippedCount,
                                 int duplicateScanCount, int invalidScanCount) {
        this.movedCount = movedCount;
        this.newAssetCount = newAssetCount;
        this.missingCount = missingCount;
        this.skippedCount = skippedCount;
        this.duplicateScanCount = duplicateScanCount;
        this.invalidScanCount = invalidScanCount;
    }

    public int getMovedCount() {
        return movedCount;
    }

    public int getNewAssetCount() {
        return newAssetCount;
    }

    public int getMissingCount() {
        return missingCount;
    }

    public int getSkippedCount() {
        return skippedCount;
    }

    public int getDuplicateScanCount() {
        return duplicateScanCount;
    }

    public int getInvalidScanCount() {
        return invalidScanCount;
    }

    public boolean hasAssetExceptions() {
        return movedCount + newAssetCount + missingCount + skippedCount > 0;
    }
}
