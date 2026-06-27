package uk.co.hsim.assetaudit.importfile;

public final class AssetFileReadException extends Exception {
    public AssetFileReadException(String message) {
        super(message);
    }

    public AssetFileReadException(String message, Throwable cause) {
        super(message, cause);
    }
}
