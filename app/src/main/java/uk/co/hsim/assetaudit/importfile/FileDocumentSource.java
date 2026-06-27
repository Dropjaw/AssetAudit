package uk.co.hsim.assetaudit.importfile;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

public final class FileDocumentSource implements DocumentSource {
    private final File file;
    private final DocumentReference reference;

    public FileDocumentSource(File file, AssetFileFormat format) {
        this.file = file;
        this.reference = new DocumentReference(null, file.getName(), "", file.length(), format);
    }

    @Override
    public DocumentReference getReference() {
        return reference;
    }

    @Override
    public InputStream openStream() throws IOException {
        return new FileInputStream(file);
    }
}
