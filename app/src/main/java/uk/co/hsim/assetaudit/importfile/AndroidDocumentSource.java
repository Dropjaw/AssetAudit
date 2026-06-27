package uk.co.hsim.assetaudit.importfile;

import android.content.ContentResolver;

import java.io.IOException;
import java.io.InputStream;

public final class AndroidDocumentSource implements DocumentSource {
    private final ContentResolver contentResolver;
    private final DocumentReference reference;

    public AndroidDocumentSource(ContentResolver contentResolver, DocumentReference reference) {
        this.contentResolver = contentResolver;
        this.reference = reference;
    }

    @Override
    public DocumentReference getReference() {
        return reference;
    }

    @Override
    public InputStream openStream() throws IOException {
        InputStream stream = contentResolver.openInputStream(reference.getUri());
        if (stream == null) {
            throw new IOException("Provider returned no stream.");
        }
        return stream;
    }
}
