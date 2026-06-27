package uk.co.hsim.assetaudit.importfile;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public final class AssetImportMapper {
    public List<AssetImportRow> mapRows(ParsedAssetFile parsedFile) {
        List<AssetImportRow> rows = new ArrayList<>();
        HeaderDetectionResult detection = detectHeaders(parsedFile);
        if (!detection.isFound()) {
            return rows;
        }
        Map<String, Integer> headers = detection.getNormalizedHeaderIndexes();
        for (RawAssetRow rawRow : parsedFile.getRows()) {
            if (rawRow.getSourceRowNumber() <= detection.getSourceRowNumber() || rawRow.isBlank()) {
                continue;
            }
            rows.add(new AssetImportRow(
                    rawRow.getSourceRowNumber(),
                    cell(rawRow, headers.get(HeaderDetector.ASSET_TAG_ID)),
                    cell(rawRow, headers.get(HeaderDetector.DEPARTMENT)),
                    cell(rawRow, headers.get(HeaderDetector.DESCRIPTION)),
                    cell(rawRow, headers.get(HeaderDetector.STATUS)),
                    cell(rawRow, headers.get(HeaderDetector.BRAND)),
                    cell(rawRow, headers.get(HeaderDetector.MODEL)),
                    cell(rawRow, headers.get(HeaderDetector.SITE)),
                    cell(rawRow, headers.get(HeaderDetector.LOCATION)),
                    cell(rawRow, headers.get(HeaderDetector.CATEGORY)),
                    cell(rawRow, headers.get(HeaderDetector.SUB_CATEGORY)),
                    cell(rawRow, headers.get(HeaderDetector.PRIMARY_USER)),
                    cell(rawRow, headers.get(HeaderDetector.OWNER)),
                    cell(rawRow, headers.get(HeaderDetector.AUDIT_DETAILS))
            ));
        }
        return rows;
    }

    public HeaderDetectionResult detectHeaders(ParsedAssetFile parsedFile) {
        return new HeaderDetector().detect(parsedFile.getRows());
    }

    private static String cell(RawAssetRow row, Integer index) {
        if (index == null || index < 0 || index >= row.getCells().size()) {
            return "";
        }
        String value = row.getCells().get(index);
        return value == null ? "" : value;
    }
}
