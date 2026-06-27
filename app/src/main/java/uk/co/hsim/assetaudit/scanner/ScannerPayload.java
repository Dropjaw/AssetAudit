package uk.co.hsim.assetaudit.scanner;

public final class ScannerPayload {
    private final String data;
    private final String symbology;
    private final long timestampUtc;
    private final String source;

    public ScannerPayload(String data, String symbology, long timestampUtc, String source) {
        this.data = data;
        this.symbology = symbology;
        this.timestampUtc = timestampUtc;
        this.source = source;
    }

    public String getData() {
        return data;
    }

    public String getSymbology() {
        return symbology;
    }

    public long getTimestampUtc() {
        return timestampUtc;
    }

    public String getSource() {
        return source;
    }
}
