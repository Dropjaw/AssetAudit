package uk.co.hsim.assetaudit.importfile;

import java.io.IOException;
import java.io.InputStream;

public interface DocumentSource {
    DocumentReference getReference();

    InputStream openStream() throws IOException;
}
