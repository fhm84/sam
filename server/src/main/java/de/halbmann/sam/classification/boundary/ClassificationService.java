package de.halbmann.sam.classification.boundary;

import de.halbmann.sam.EnvConsts;
import de.halbmann.sam.classification.controller.DocumentUtils;
import de.halbmann.sam.classification.entity.SheetAnalyzerResult;
import dev.langchain4j.data.image.Image;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
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
import org.eclipse.microprofile.config.inject.ConfigProperty;

@Slf4j
@ApplicationScoped
@Transactional
public class ClassificationService {

    private final SheetAnalyzer analyzer;
    private final MeterRegistry registry;
    private final boolean debug;

    @Inject
    public ClassificationService(
            SheetAnalyzer analyzer,
            MeterRegistry registry,
            @ConfigProperty(name = EnvConsts.CLASSIFICATION_DEBUG, defaultValue = "false") boolean debug) {
        this.analyzer = analyzer;
        this.registry = registry;
        this.debug = debug;
    }

    public SheetAnalyzerResult analyzePdf(InputStream fileInputStream) {
        try {
            byte[] pdfBytes = fileInputStream.readAllBytes();

            // Try text extraction first — no vision tokens needed for digital PDFs
            String extractedText = DocumentUtils.extractText(new ByteArrayInputStream(pdfBytes));
            if (isMeaningfulText(extractedText)) {
                log.debug("PDF text extraction succeeded ({} chars), using text path", extractedText.length());
                return time("text", () -> analyzer.analyzeText(extractedText));
            }

            // Scanned / image-only PDF: fall back to vision
            log.debug("PDF text extraction insufficient, falling back to vision path");
            try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
                DocumentUtils.pdfToImage(new ByteArrayInputStream(pdfBytes), baos);
                if (debug) {
                    Files.write(Files.createTempFile("sam-sheet-upload", ".png"), baos.toByteArray());
                }
                return time("vision", () -> analyzeImageInternal(new ByteArrayInputStream(baos.toByteArray())));
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static boolean isMeaningfulText(String text) {
        if (text == null) {
            return false;
        }
        long printable = text.chars().filter(c -> c > 32 && c != 65533).count();
        return printable >= 50;
    }

    public SheetAnalyzerResult analyzeImage(InputStream fileInputStream) {
        return time("vision", () -> analyzeImageInternal(fileInputStream));
    }

    private SheetAnalyzerResult analyzeImageInternal(InputStream fileInputStream) {
        return analyzer.analyze(Image.builder()
                .base64Data(encodeFileToBase64(fileInputStream))
                .mimeType("image/png")
                .build());
    }

    private SheetAnalyzerResult time(String mode, java.util.concurrent.Callable<SheetAnalyzerResult> action) {
        Timer timer = Timer.builder("sam.classification.duration")
                .tag("mode", mode)
                .description("Time spent on AI-based sheet classification")
                .register(registry);
        registry.counter("sam.classification.requests", "mode", mode).increment();
        try {
            return timer.recordCallable(action);
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    String encodeFileToBase64(InputStream fileInputStream) {
        try {
            return Base64.getEncoder().encodeToString(fileInputStream.readAllBytes());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
