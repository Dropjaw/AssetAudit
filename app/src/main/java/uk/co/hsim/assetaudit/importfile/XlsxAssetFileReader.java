package uk.co.hsim.assetaudit.importfile;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.xml.sax.SAXException;

public final class XlsxAssetFileReader implements AssetFileReader {
    @Override
    public boolean canRead(AssetFileFormat format) {
        return format == AssetFileFormat.XLSX;
    }

    @Override
    public ParsedAssetFile read(DocumentSource source) throws AssetFileReadException {
        try {
            Map<String, byte[]> entries = readZipEntries(source.openStream());
            List<String> sharedStrings = readSharedStrings(entries.get("xl/sharedStrings.xml"));
            String sheetPath = firstVisibleSheetPath(entries);
            byte[] sheetBytes = entries.get(sheetPath);
            if (sheetBytes == null) {
                throw new AssetFileReadException("Workbook does not contain a readable first worksheet.");
            }
            List<RawAssetRow> rows = readSheetRows(sheetBytes, sharedStrings);
            HeaderDetectionResult header = new HeaderDetector().detect(rows);
            List<String> headers = header.isFound() ? rows.get(header.getSourceRowNumber() - 1).getCells() : new ArrayList<>();
            String auditName = detectAuditName(rows, header.getSourceRowNumber());
            AssetFileMetadata metadata = new AssetFileMetadata(auditName, source.getReference().getDisplayName(),
                    AssetFileFormat.XLSX, rows.size());
            return new ParsedAssetFile(metadata, header.getSourceRowNumber(), headers, rows);
        } catch (IOException e) {
            throw new AssetFileReadException("Unable to read XLSX file.", e);
        }
    }

    private static Map<String, byte[]> readZipEntries(InputStream inputStream) throws IOException {
        Map<String, byte[]> entries = new HashMap<>();
        try (ZipInputStream zip = new ZipInputStream(inputStream)) {
            ZipEntry entry;
            byte[] buffer = new byte[8192];
            while ((entry = zip.getNextEntry()) != null) {
                if (entry.isDirectory()) {
                    continue;
                }
                ByteArrayOutputStream out = new ByteArrayOutputStream();
                int read;
                while ((read = zip.read(buffer)) != -1) {
                    out.write(buffer, 0, read);
                }
                entries.put(entry.getName(), out.toByteArray());
            }
        }
        return entries;
    }

    private static String firstVisibleSheetPath(Map<String, byte[]> entries) throws AssetFileReadException {
        byte[] workbookBytes = entries.get("xl/workbook.xml");
        byte[] relsBytes = entries.get("xl/_rels/workbook.xml.rels");
        if (workbookBytes == null || relsBytes == null) {
            return "xl/worksheets/sheet1.xml";
        }
        Document workbook = parseXml(workbookBytes);
        Document rels = parseXml(relsBytes);
        Map<String, String> relationshipTargets = new HashMap<>();
        NodeList relationships = rels.getElementsByTagName("Relationship");
        for (int i = 0; i < relationships.getLength(); i++) {
            Element rel = (Element) relationships.item(i);
            relationshipTargets.put(rel.getAttribute("Id"), rel.getAttribute("Target"));
        }
        NodeList sheets = workbook.getElementsByTagName("sheet");
        for (int i = 0; i < sheets.getLength(); i++) {
            Element sheet = (Element) sheets.item(i);
            if ("hidden".equalsIgnoreCase(sheet.getAttribute("state"))) {
                continue;
            }
            String relationshipId = sheet.getAttribute("r:id");
            String target = relationshipTargets.get(relationshipId);
            if (target != null && !target.isEmpty()) {
                return target.startsWith("/") ? target.substring(1) : "xl/" + target;
            }
        }
        return "xl/worksheets/sheet1.xml";
    }

    private static List<String> readSharedStrings(byte[] sharedStringBytes) throws AssetFileReadException {
        List<String> values = new ArrayList<>();
        if (sharedStringBytes == null) {
            return values;
        }
        Document document = parseXml(sharedStringBytes);
        NodeList items = document.getElementsByTagName("si");
        for (int i = 0; i < items.getLength(); i++) {
            Element item = (Element) items.item(i);
            NodeList textNodes = item.getElementsByTagName("t");
            StringBuilder value = new StringBuilder();
            for (int j = 0; j < textNodes.getLength(); j++) {
                value.append(textNodes.item(j).getTextContent());
            }
            values.add(value.toString());
        }
        return values;
    }

    private static List<RawAssetRow> readSheetRows(byte[] sheetBytes, List<String> sharedStrings)
            throws AssetFileReadException {
        List<RawAssetRow> rows = new ArrayList<>();
        Document document = parseXml(sheetBytes);
        NodeList rowNodes = document.getElementsByTagName("row");
        for (int i = 0; i < rowNodes.getLength(); i++) {
            Element row = (Element) rowNodes.item(i);
            int rowNumber = parsePositiveInt(row.getAttribute("r"), i + 1);
            List<String> cells = new ArrayList<>();
            NodeList cellNodes = row.getElementsByTagName("c");
            for (int j = 0; j < cellNodes.getLength(); j++) {
                Element cell = (Element) cellNodes.item(j);
                int columnIndex = columnIndex(cell.getAttribute("r"), j);
                while (cells.size() <= columnIndex) {
                    cells.add("");
                }
                cells.set(columnIndex, readCellValue(cell, sharedStrings));
            }
            rows.add(new RawAssetRow(rowNumber, cells));
        }
        return rows;
    }

    private static String readCellValue(Element cell, List<String> sharedStrings) {
        String type = cell.getAttribute("t");
        if ("inlineStr".equals(type)) {
            NodeList inlineText = cell.getElementsByTagName("t");
            return inlineText.getLength() == 0 ? "" : inlineText.item(0).getTextContent();
        }
        String raw = childText(cell, "v");
        if ("s".equals(type)) {
            int index = parsePositiveInt(raw, -1);
            return index >= 0 && index < sharedStrings.size() ? sharedStrings.get(index) : "";
        }
        return raw == null ? "" : raw;
    }

    private static String detectAuditName(List<RawAssetRow> rows, int headerRowNumber) {
        for (RawAssetRow row : rows) {
            if (headerRowNumber > 0 && row.getSourceRowNumber() >= headerRowNumber) {
                break;
            }
            for (String cell : row.getCells()) {
                String value = cell == null ? "" : cell.trim();
                if (value.toLowerCase(Locale.ROOT).startsWith("audit name:")) {
                    return value.substring("audit name:".length()).trim();
                }
            }
        }
        return "";
    }

    private static String childText(Element element, String tagName) {
        NodeList nodes = element.getElementsByTagName(tagName);
        return nodes.getLength() == 0 ? "" : nodes.item(0).getTextContent();
    }

    private static int columnIndex(String cellReference, int fallback) {
        if (cellReference == null || cellReference.isEmpty()) {
            return fallback;
        }
        int result = 0;
        boolean found = false;
        for (int i = 0; i < cellReference.length(); i++) {
            char c = cellReference.charAt(i);
            if (c >= 'A' && c <= 'Z') {
                found = true;
                result = result * 26 + (c - 'A' + 1);
            } else if (c >= 'a' && c <= 'z') {
                found = true;
                result = result * 26 + (c - 'a' + 1);
            } else {
                break;
            }
        }
        return found ? result - 1 : fallback;
    }

    private static int parsePositiveInt(String value, int fallback) {
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    private static Document parseXml(byte[] bytes) throws AssetFileReadException {
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(false);
            setFeatureIfSupported(factory, "http://apache.org/xml/features/disallow-doctype-decl", true);
            return factory.newDocumentBuilder().parse(new ByteArrayInputStream(bytes));
        } catch (ParserConfigurationException | SAXException | IOException e) {
            throw new AssetFileReadException("Unable to parse workbook XML.", e);
        }
    }

    private static void setFeatureIfSupported(DocumentBuilderFactory factory, String feature, boolean enabled)
            throws ParserConfigurationException {
        try {
            factory.setFeature(feature, enabled);
        } catch (ParserConfigurationException e) {
            String message = e.getMessage();
            if (message != null && message.toLowerCase(Locale.ROOT).contains("not recognized")) {
                return;
            }
            if (message != null && message.toLowerCase(Locale.ROOT).contains("not supported")) {
                return;
            }
            throw e;
        }
    }
}
