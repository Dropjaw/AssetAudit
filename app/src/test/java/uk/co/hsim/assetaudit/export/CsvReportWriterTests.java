package uk.co.hsim.assetaudit.export;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;

public class CsvReportWriterTests {
    @Test
    public void writerKeepsLeadingZeroesAndEscapesCsvFields() {
        CsvReportWriter writer = new CsvReportWriter();

        byte[] bytes = writer.write(Arrays.asList(
                new String[]{"asset_tag_id", "description", "owner"},
                new String[]{"0011", "Monitor, 22 inch", "A \"User\""}
        ), false);

        String csv = new String(bytes, StandardCharsets.UTF_8);
        assertEquals("asset_tag_id,description,owner\r\n0011,\"Monitor, 22 inch\",\"A \"\"User\"\"\"\r\n", csv);
    }

    @Test
    public void excelFriendlyCsvAddsBomAndNeutralisesFormulaValues() {
        CsvReportWriter writer = new CsvReportWriter();

        byte[] bytes = writer.write(Arrays.asList(
                new String[]{"asset_tag_id", "notes"},
                new String[]{"0069", "=HYPERLINK(\"x\")"}
        ), true);

        assertEquals((byte) 0xEF, bytes[0]);
        assertEquals((byte) 0xBB, bytes[1]);
        assertEquals((byte) 0xBF, bytes[2]);
        String csv = new String(bytes, StandardCharsets.UTF_8);
        assertTrue(csv.contains("0069,\"'=HYPERLINK(\"\"x\"\")\""));
    }
}
