package de.halbmann.sam.classification.boundary;

import de.halbmann.sam.classification.controller.DocumentUtils;
import de.halbmann.sam.classification.entity.SheetAnalyzerResult;
import dev.langchain4j.data.image.Image;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.Base64;

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
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            // in case of a pdf sheet, we first "prepare" the pdf for analyzing (we just need the top and
            // bottom parts
            // of the file ... and delegate to image analyzation then)
            DocumentUtils.pdfToImage(fileInputStream, baos);
            // in case of debugging, we write out the (temporary) generated image file
            // TODO: write out generated image file in case of debugging
            if (debug) {
                Files.write(Files.createTempFile("sam-sheet-upload", ".png"), baos.toByteArray());
            }

            return analyzeImage(IOUtils.copy(baos));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
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
