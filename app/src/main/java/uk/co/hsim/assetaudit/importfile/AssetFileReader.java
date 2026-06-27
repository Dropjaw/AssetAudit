package uk.co.hsim.assetaudit.importfile;

public interface AssetFileReader {
    boolean canRead(AssetFileFormat format);

    ParsedAssetFile read(DocumentSource source) throws AssetFileReadException;
}
