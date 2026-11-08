package de.halbmann.sam.storage.upload;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import de.halbmann.sam.core.exception.ValidationException;
import de.halbmann.sam.core.storage.upload.UploadContext;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class ContentTypePolicyTest {

    private final ContentTypePolicy policy =
            new ContentTypePolicy(List.of("application/pdf", " Image/PNG ", "audio/*", ""));

    @ParameterizedTest
    @ValueSource(strings = {"application/pdf", "image/png", "IMAGE/PNG", "audio/mpeg", "audio/midi"})
    void listedTypesAndFamilies_areAllowed(String type) {
        assertTrue(policy.isAllowed(type));
    }

    @ParameterizedTest
    @ValueSource(
            strings = {
                "text/html",
                "image/svg+xml",
                "image/jpeg",
                "application/x-msdownload",
                "application/zip",
                "application/octet-stream",
                "audio",
                "audiox/mpeg"
            })
    void otherTypes_areRefused(String type) {
        assertFalse(policy.isAllowed(type));
    }

    @Test
    void unknownType_isRefused() {
        assertFalse(policy.isAllowed(null));
    }

    @Test
    void wildcard_allowsEverything() {
        assertTrue(new ContentTypePolicy(List.of("*/*")).isAllowed("application/x-msdownload"));
    }

    @Test
    void verify_refusesWithAClientError() {
        assertThrows(ValidationException.class, () -> policy.verify(new UploadContext("page.pdf", 10, "text/html")));
        assertDoesNotThrow(() -> policy.verify(new UploadContext("scan.pdf", 10, "application/pdf")));
    }
}
