package uk.co.hsim.assetaudit.importfile;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

final class InMemoryDocumentSource implements DocumentSource {
    private final byte[] bytes;
    private final DocumentReference reference;

    InMemoryDocumentSource(String displayName, String mimeType, String content) {
        this.bytes = content.getBytes(StandardCharsets.UTF_8);
        AssetFileFormat format = AssetFileFormatDetector.detect(displayName, mimeType);
        this.reference = new DocumentReference(null, displayName, mimeType, bytes.length, format);
    }

    @Override
    public DocumentReference getReference() {
        return reference;
    }

    @Override
    public InputStream openStream() {
        return new ByteArrayInputStream(bytes);
    }
}
