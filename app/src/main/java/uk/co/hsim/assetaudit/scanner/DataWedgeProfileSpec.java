package uk.co.hsim.assetaudit.scanner;

public final class DataWedgeProfileSpec {
    private final DataWedgeProfileConfig config;
    private final boolean barcodeInputEnabled;
    private final boolean keystrokeOutputEnabled;
    private final boolean intentOutputEnabled;
    private final String configMode;
    private final String intentDelivery;

    public DataWedgeProfileSpec(DataWedgeProfileConfig config, boolean barcodeInputEnabled,
                                boolean keystrokeOutputEnabled, boolean intentOutputEnabled,
                                String configMode, String intentDelivery) {
        this.config = config;
        this.barcodeInputEnabled = barcodeInputEnabled;
        this.keystrokeOutputEnabled = keystrokeOutputEnabled;
        this.intentOutputEnabled = intentOutputEnabled;
        this.configMode = configMode;
        this.intentDelivery = intentDelivery;
    }

    public DataWedgeProfileConfig getConfig() {
        return config;
    }

    public boolean isBarcodeInputEnabled() {
        return barcodeInputEnabled;
    }

    public boolean isKeystrokeOutputEnabled() {
        return keystrokeOutputEnabled;
    }

    public boolean isIntentOutputEnabled() {
        return intentOutputEnabled;
    }

    public String getConfigMode() {
        return configMode;
    }

    public String getIntentDelivery() {
        return intentDelivery;
    }
}
