package de.halbmann.sam.core.controller;

import static org.junit.jupiter.api.Assertions.*;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import org.junit.jupiter.api.Test;

class MimeTypeDetectorTest {

    private final MimeTypeDetector detector = new MimeTypeDetector();

    @Test
    void detectsPdfByMagicBytes() {
        // %PDF- magic header
        byte[] pdfMagic = "%PDF-1.4 %fake content".getBytes();
        BufferedInputStream in = new BufferedInputStream(new ByteArrayInputStream(pdfMagic));
        String mime = detector.detectMimeType(in, "document.pdf");
        assertEquals("application/pdf", mime);
    }

    @Test
    void detectsPlainTextByFilename() {
        byte[] content = "hello world".getBytes();
        BufferedInputStream in = new BufferedInputStream(new ByteArrayInputStream(content));
        String mime = detector.detectMimeType(in, "readme.txt");
        assertTrue(mime.startsWith("text/"), "expected text/* but got: " + mime);
    }

    @Test
    void detectsPngByMagicBytes() {
        // PNG magic: \x89PNG\r\n\x1a\n
        byte[] pngMagic = new byte[] {(byte) 0x89, 'P', 'N', 'G', '\r', '\n', 0x1a, '\n'};
        BufferedInputStream in = new BufferedInputStream(new ByteArrayInputStream(pngMagic));
        String mime = detector.detectMimeType(in, "image.png");
        assertEquals("image/png", mime);
    }
}
