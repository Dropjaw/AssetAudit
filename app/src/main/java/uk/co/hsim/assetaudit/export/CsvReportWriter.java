package uk.co.hsim.assetaudit.export;

import java.nio.charset.StandardCharsets;
import java.util.List;

public final class CsvReportWriter {
    public byte[] write(List<String[]> rows, boolean excelFriendly) {
        StringBuilder builder = new StringBuilder();
        if (excelFriendly) {
            builder.append('\uFEFF');
        }
        for (String[] row : rows) {
            for (int i = 0; i < row.length; i++) {
                if (i > 0) {
                    builder.append(',');
                }
                builder.append(escape(row[i]));
            }
            builder.append("\r\n");
        }
        return builder.toString().getBytes(StandardCharsets.UTF_8);
    }

    String escape(String value) {
        String safe = neutralize(value == null ? "" : value).replace("\"", "\"\"");
        boolean quote = safe.contains(",") || safe.contains("\"") || safe.contains("\n") || safe.contains("\r");
        return quote ? "\"" + safe + "\"" : safe;
    }

    private String neutralize(String value) {
        if (value.isEmpty()) {
            return value;
        }
        char first = value.charAt(0);
        return first == '=' || first == '+' || first == '-' || first == '@' ? "'" + value : value;
    }
}
