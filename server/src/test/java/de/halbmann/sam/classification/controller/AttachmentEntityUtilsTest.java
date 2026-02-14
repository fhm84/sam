package de.halbmann.sam.classification.controller;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import org.junit.jupiter.api.Test;

class AttachmentEntityUtilsTest {

    @Test
    void shouldConvertPdfToImage() throws IOException {
        Path pngFile = Paths.get("", "src", "test", "resources", "test3.pdf");
        try (InputStream inputStream = Files.newInputStream(pngFile);
                OutputStream target = Files.newOutputStream(Paths.get("", "target", "test3.png"))) {
            DocumentUtils.pdfToImage(inputStream, target);
        }
    }
}
