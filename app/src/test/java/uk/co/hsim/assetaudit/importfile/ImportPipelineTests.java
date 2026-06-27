package uk.co.hsim.assetaudit.importfile;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.io.File;
import java.net.URL;
import java.util.List;

import uk.co.hsim.assetaudit.domain.enums.ImportIssueSeverity;

public class ImportPipelineTests {
    @Test
    public void formatDetectorFindsCsvXlsxAndUnsupported() {
        assertEquals(AssetFileFormat.CSV, AssetFileFormatDetector.detect("assets.csv", "text/csv"));
        assertEquals(AssetFileFormat.XLSX, AssetFileFormatDetector.detect("NonAuditedAssets.xlsx", ""));
        assertEquals(AssetFileFormat.UNSUPPORTED, AssetFileFormatDetector.detect("assets.txt", "text/plain"));
    }

    @Test
    public void csvReaderHandlesBomQuotesCommasTrailingBlanksAndLeadingZeroes() throws Exception {
        String csv = "\uFEFFAudit Name: CSV Test\n"
                + "Asset Tag ID,Department,Description,Status,Site,Location,Category,Sub Category,Owner\n"
                + "0011,IT,\"Monitor, 22 inch\",Available,Poole,Unit 11,Computer equipment,,\n";
        ParsedAssetFile parsed = new CsvAssetFileReader().read(new InMemoryDocumentSource("assets.csv", "text/csv", csv));
        List<AssetImportRow> rows = new AssetImportMapper().mapRows(parsed);

        assertEquals(2, parsed.getHeaderRowNumber());
        assertEquals("0011", rows.get(0).getAssetTagId());
        assertEquals("Monitor, 22 inch", rows.get(0).getDescription());
        assertEquals("", rows.get(0).getSubCategory());
        assertEquals("", rows.get(0).getOwner());
    }

    @Test
    public void xlsxFixtureReadsExpectedWorkbookShape() throws Exception {
        ParsedAssetFile parsed = readFixture();
        List<AssetImportRow> rows = new AssetImportMapper().mapRows(parsed);

        assertEquals(2, parsed.getHeaderRowNumber());
        assertEquals(13, parsed.getHeaders().size());
        assertEquals(569, rows.size());
        assertEquals("0011", rows.get(0).getAssetTagId());
        assertEquals("NN93N061612303C1F", rows.get(rows.size() - 1).getAssetTagId());
        assertTrue(parsed.getMetadata().getAuditName().contains("2025 Audit"));
    }

    @Test
    public void headerDetectionHandlesMetadataAndReorderedColumns() {
        ParsedAssetFile parsed = new ParsedAssetFile(
                new AssetFileMetadata("", "memory.csv", AssetFileFormat.CSV, 3),
                2,
                java.util.Arrays.asList("Department", "Category", "Location", "Site", "Status", "Description", "Asset Tag ID"),
                java.util.Arrays.asList(
                        new RawAssetRow(1, java.util.Arrays.asList("Audit Name: Test")),
                        new RawAssetRow(2, java.util.Arrays.asList("Department", "Category", "Location", "Site", "Status", "Description", "Asset Tag ID")),
                        new RawAssetRow(3, java.util.Arrays.asList("IT", "Computer equipment", "Unit 11", "Poole", "Available", "Switch", "0069"))
                )
        );
        HeaderDetectionResult result = new HeaderDetector().detect(parsed.getRows());
        List<AssetImportRow> rows = new AssetImportMapper().mapRows(parsed);

        assertTrue(result.isFound());
        assertEquals(2, result.getSourceRowNumber());
        assertEquals("0069", rows.get(0).getAssetTagId());
        assertEquals("IT", rows.get(0).getDepartment());
    }

    @Test
    public void validatorBlocksDuplicatesMissingHeadersAndBlankRequiredFields() throws Exception {
        String csv = "Asset Tag ID,Department,Description,Status,Site,Location,Category\n"
                + "0011,IT,Switch,Available,Poole,Unit 11,Computer equipment\n"
                + "0011,IT,Switch,Available,Poole,Unit 11,Computer equipment\n"
                + ",IT,Switch,Available,Poole,Unit 11,Computer equipment\n"
                + "0099,,Switch,Available,Poole,Unit 11,Computer equipment\n";
        ParsedAssetFile parsed = new CsvAssetFileReader().read(new InMemoryDocumentSource("assets.csv", "text/csv", csv));
        AssetImportMapper mapper = new AssetImportMapper();
        ImportValidationResult result = new AssetImportValidator().validate(
                parsed,
                mapper.mapRows(parsed),
                mapper.detectHeaders(parsed),
                ImportSettings.defaults("Unassigned / Blank Department")
        );

        assertFalse(result.isImportable());
        assertTrue(result.getFatalCount() >= 2);
        assertTrue(result.getWarningCount() >= 1);
    }

    @Test
    public void previewCountsMatchWorkbookFacts() throws Exception {
        ParsedAssetFile parsed = readFixture();
        AssetImportMapper mapper = new AssetImportMapper();
        HeaderDetectionResult headers = mapper.detectHeaders(parsed);
        List<AssetImportRow> rows = mapper.mapRows(parsed);
        ImportSettings settings = ImportSettings.defaults("Unassigned / Blank Department");
        ImportValidationResult validation = new AssetImportValidator().validate(parsed, rows, headers, settings);
        ImportPreview preview = new ImportPreviewBuilder().build(
                new DocumentReference(null, "NonAuditedAssets.xlsx", "", 0, AssetFileFormat.XLSX),
                parsed,
                headers,
                validation,
                settings
        );

        assertTrue(validation.isImportable());
        assertEquals(569, validation.getAcceptedRows().size());
        assertEquals(0, preview.getDuplicateTagCount());
        assertEquals(Integer.valueOf(326), preview.getDepartmentCounts().get("IT"));
        assertEquals(Integer.valueOf(2), preview.getDepartmentCounts().get("Unassigned / Blank Department"));
        assertEquals(31, preview.getLeadingZeroTagCount());
        assertEquals(328, preview.getAlphanumericTagCount());
        assertEquals(2, countIssues(validation, ImportIssueCode.DEPARTMENT_BLANK));
        assertEquals(39, countIssues(validation, ImportIssueCode.PRIOR_AUDIT_DETAILS_PRESENT));
    }

    private ParsedAssetFile readFixture() throws Exception {
        URL url = getClass().getClassLoader().getResource("fixtures/NonAuditedAssets.xlsx");
        assertNotNull(url);
        return new XlsxAssetFileReader().read(new FileDocumentSource(new File(url.toURI()), AssetFileFormat.XLSX));
    }

    private int countIssues(ImportValidationResult result, ImportIssueCode code) {
        int count = 0;
        for (ImportIssue issue : result.getIssues()) {
            if (issue.getCode() == code && issue.getSeverity() != ImportIssueSeverity.FATAL) {
                count++;
            }
        }
        return count;
    }
}
