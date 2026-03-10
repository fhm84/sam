package de.halbmann.sam.classification.boundary;

import de.halbmann.sam.classification.controller.DocumentUtils;
import de.halbmann.sam.classification.entity.SheetAnalyzerResult;
import dev.langchain4j.data.image.Image;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.Base64;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@ApplicationScoped
@Transactional
public class ClassificationService {

    private final SheetAnalyzer analyzer;

    private final boolean debug = true;

    @Inject
    public ClassificationService(SheetAnalyzer analyzer) {
        this.analyzer = analyzer;
    }

    public SheetAnalyzerResult analyzePdf(InputStream fileInputStream) {
        try {
            byte[] pdfBytes = fileInputStream.readAllBytes();

            // Try text extraction first — no vision tokens needed for digital PDFs
            String extractedText = DocumentUtils.extractText(new ByteArrayInputStream(pdfBytes));
            if (isMeaningfulText(extractedText)) {
                log.debug("PDF text extraction succeeded ({} chars), using text path", extractedText.length());
                return analyzer.analyzeText(extractedText);
            }

            // Scanned / image-only PDF: fall back to vision
            log.debug("PDF text extraction insufficient, falling back to vision path");
            try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
                DocumentUtils.pdfToImage(new ByteArrayInputStream(pdfBytes), baos);
                if (debug) {
                    Files.write(Files.createTempFile("sam-sheet-upload", ".png"), baos.toByteArray());
                }
                return analyzeImage(new ByteArrayInputStream(baos.toByteArray()));
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static boolean isMeaningfulText(String text) {
        if (text == null) return false;
        long printable = text.chars().filter(c -> c > 32 && c != 65533).count();
        return printable >= 50;
    }

    public SheetAnalyzerResult analyzeImage(InputStream fileInputStream) {
        return analyzer.analyze(Image.builder()
                .base64Data(encodeFileToBase64(fileInputStream))
                .mimeType("image/png")
                .build());
    }

    String encodeFileToBase64(InputStream fileInputStream) {
        try {
            return Base64.getEncoder().encodeToString(fileInputStream.readAllBytes());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
