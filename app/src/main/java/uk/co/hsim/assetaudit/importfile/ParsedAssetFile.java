package uk.co.hsim.assetaudit.importfile;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class ParsedAssetFile {
    private final AssetFileMetadata metadata;
    private final int headerRowNumber;
    private final List<String> headers;
    private final List<RawAssetRow> rows;

    public ParsedAssetFile(AssetFileMetadata metadata, int headerRowNumber, List<String> headers,
                           List<RawAssetRow> rows) {
        this.metadata = metadata;
        this.headerRowNumber = headerRowNumber;
        this.headers = Collections.unmodifiableList(new ArrayList<>(headers));
        this.rows = Collections.unmodifiableList(new ArrayList<>(rows));
    }

    public AssetFileMetadata getMetadata() {
        return metadata;
    }

    public int getHeaderRowNumber() {
        return headerRowNumber;
    }

    public List<String> getHeaders() {
        return headers;
    }

    public List<RawAssetRow> getRows() {
        return rows;
    }
}
