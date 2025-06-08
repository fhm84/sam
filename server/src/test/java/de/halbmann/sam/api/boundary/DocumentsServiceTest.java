package de.halbmann.sam.api.boundary;

import de.halbmann.sam.api.entity.Clef;
import de.halbmann.sam.business.boundary.DocumentsService;
import de.halbmann.sam.classification.entity.SheetAnalyzerResult;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.*;

@QuarkusTest
class DocumentsServiceTest {

    @Inject
    DocumentsService documentsService;

    @Test
    void shouldAnalyzePdfDocument() throws IOException {
        Path pdfFile = Paths.get("", "src", "main", "resources", "test.pdf");
        assertTrue(Files.exists(pdfFile));
        SheetAnalyzerResult sheetAnalyzerResult = documentsService.analyzePdf(Files.newInputStream(pdfFile));
        System.out.println(sheetAnalyzerResult);
        assertNotNull(sheetAnalyzerResult);
        assertNotNull(sheetAnalyzerResult.instrumentation());
        assertAll(() -> {
            assertEquals("Polka ins Glück", sheetAnalyzerResult.title());
            assertEquals("Anton Gälle", sheetAnalyzerResult.composer());
            assertEquals("Mathias Gronert", sheetAnalyzerResult.arranger());
            assertEquals("EGOTON Label & Musikverlag, Inh. Mathias Gronert, Burgstr. 8a D-78132 Hornberg", sheetAnalyzerResult.publisher());
            assertEquals(2015, sheetAnalyzerResult.yearOfComposition());

            assertEquals("Tenorhorn", sheetAnalyzerResult.instrumentation().instrumentName());
            assertEquals("1.", sheetAnalyzerResult.instrumentation().partLabel());
            assertEquals("Bb", sheetAnalyzerResult.instrumentation().transposition());
            assertEquals(Clef.TREBLE, sheetAnalyzerResult.instrumentation().clef());
        });
    }

    @Test
    void shouldAnalyzePngDocument() throws IOException {
        Path pngFile = Paths.get("", "target", "test3.png");
        assertTrue(Files.exists(pngFile));
        SheetAnalyzerResult sheetAnalyzerResult = documentsService.analyzeImage(Files.newInputStream(pngFile));
        assertNotNull(sheetAnalyzerResult);
        System.out.println(sheetAnalyzerResult);
        assertEquals("Sucker", sheetAnalyzerResult.title());
        //assertEquals("Rauschende Birken", sheetAnalyzerResult.title());
        //assertEquals("Polka ins Glück", sheetAnalyzerResult.title());
    }

}
