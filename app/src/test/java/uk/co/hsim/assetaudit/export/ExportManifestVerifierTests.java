package uk.co.hsim.assetaudit.export;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.util.Collections;

public class ExportManifestVerifierTests {
    private final ExportManifestVerifier verifier = new ExportManifestVerifier();

    @Test
    public void acceptsSafeFileRecord() {
        assertTrue(verifier.verify(Collections.singletonList(new ExportFileRecord(
                "updated_assets.csv",
                "text/csv",
                1,
                "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa"
        ))).isSuccess());
    }

    @Test
    public void rejectsUnsafeFileNameAndBadHash() {
        assertFalse(verifier.verify(Collections.singletonList(new ExportFileRecord(
                "../updated_assets.csv",
                "text/csv",
                1,
                "not-a-hash"
        ))).isSuccess());
    }
}
