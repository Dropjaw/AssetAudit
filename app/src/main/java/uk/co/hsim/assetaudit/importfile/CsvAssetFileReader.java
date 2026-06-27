package uk.co.hsim.assetaudit.importfile;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public final class CsvAssetFileReader implements AssetFileReader {
    @Override
    public boolean canRead(AssetFileFormat format) {
        return format == AssetFileFormat.CSV;
    }

    @Override
    public ParsedAssetFile read(DocumentSource source) throws AssetFileReadException {
        List<RawAssetRow> rawRows = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(source.openStream(), StandardCharsets.UTF_8))) {
            String line;
            int rowNumber = 1;
            while ((line = reader.readLine()) != null) {
                if (rowNumber == 1 && line.startsWith("\uFEFF")) {
                    line = line.substring(1);
                }
                rawRows.add(new RawAssetRow(rowNumber, parseLine(line)));
                rowNumber++;
            }
        } catch (IOException e) {
            throw new AssetFileReadException("Unable to read CSV file.", e);
        }
        HeaderDetectionResult header = new HeaderDetector().detect(rawRows);
        List<String> headers = header.isFound() ? rawRows.get(header.getSourceRowNumber() - 1).getCells() : new ArrayList<>();
        AssetFileMetadata metadata = new AssetFileMetadata("", source.getReference().getDisplayName(),
                AssetFileFormat.CSV, rawRows.size());
        return new ParsedAssetFile(metadata, header.getSourceRowNumber(), headers, rawRows);
    }

    static List<String> parseLine(String line) throws AssetFileReadException {
        List<String> values = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean quoted = false;
        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (c == '"') {
                if (quoted && i + 1 < line.length() && line.charAt(i + 1) == '"') {
                    current.append('"');
                    i++;
                } else {
                    quoted = !quoted;
                }
            } else if (c == ',' && !quoted) {
                values.add(current.toString());
                current.setLength(0);
            } else {
                current.append(c);
            }
        }
        if (quoted) {
            throw new AssetFileReadException("Malformed CSV row with unterminated quote.");
        }
        values.add(current.toString());
        return values;
    }
}
