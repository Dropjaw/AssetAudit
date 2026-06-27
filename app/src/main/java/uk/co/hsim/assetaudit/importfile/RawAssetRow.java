package uk.co.hsim.assetaudit.importfile;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class RawAssetRow {
    private final int sourceRowNumber;
    private final List<String> cells;

    public RawAssetRow(int sourceRowNumber, List<String> cells) {
        this.sourceRowNumber = sourceRowNumber;
        this.cells = Collections.unmodifiableList(new ArrayList<>(cells));
    }

    public int getSourceRowNumber() {
        return sourceRowNumber;
    }

    public List<String> getCells() {
        return cells;
    }

    public boolean isBlank() {
        for (String cell : cells) {
            if (cell != null && !cell.trim().isEmpty()) {
                return false;
            }
        }
        return true;
    }
}
