package uk.co.hsim.assetaudit.util.logging;

public interface AppLogger {
    void info(String component, String message);

    void warning(String component, String message);

    void error(String component, String message, Throwable throwable);
}
