package de.halbmann.sam.classification;

import static io.restassured.RestAssured.given;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import de.halbmann.sam.EnvConsts;
import de.halbmann.sam.classification.boundary.ClassificationService;
import de.halbmann.sam.classification.entity.SheetAnalyzerResult;
import io.quarkus.narayana.jta.QuarkusTransaction;
import io.quarkus.test.junit.QuarkusMock;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.security.TestSecurity;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicReference;
import javax.imageio.ImageIO;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * {@code POST /documents/{id}/classify} must not hold a database transaction while waiting for
 * the LLM (it used to: both classification services were {@code @Transactional} at class level),
 * and agentic mode must be off unless an environment enables it.
 *
 * <p>The LLM is faked at the {@link ClassificationService} level (the bean that calls the model);
 * the generated AI-service implementation itself can't be replaced with {@link QuarkusMock}.
 */
@QuarkusTest
@TestSecurity(
        user = "librarian1",
        roles = {"music_librarian"})
class ClassifyWithoutTransactionTest {

    @ConfigProperty(name = EnvConsts.CLASSIFICATION_AGENTIC)
    boolean agenticMode;

    private final AtomicReference<Boolean> transactionActiveDuringLlmCall = new AtomicReference<>();

    @BeforeEach
    void fakeTheLlm() {
        ClassificationService llm = mock(ClassificationService.class);
        when(llm.analyzeImage(any())).thenAnswer(inv -> {
            transactionActiveDuringLlmCall.set(QuarkusTransaction.isActive());
            return new SheetAnalyzerResult("Fake Title", null, null, null, null, null, null, null, null, null);
        });
        QuarkusMock.installMockForType(llm, ClassificationService.class);
    }

    @Test
    void agenticMode_isOffByDefault() {
        assertFalse(agenticMode);
    }

    @Test
    void classify_callsLlmOutsideTransaction() throws Exception {
        String documentId = given().multiPart("file", "scan.png", tinyPng(), "image/png")
                .post("/api/documents")
                .then()
                .statusCode(200)
                .extract()
                .path("document.id");

        String suggestedTitle = given().post("/api/documents/{id}/classify", documentId)
                .then()
                .statusCode(200)
                .extract()
                .path("suggested.title");

        assertEquals("Fake Title", suggestedTitle);
        assertEquals(Boolean.FALSE, transactionActiveDuringLlmCall.get(), "LLM called inside a transaction");
    }

    /** A random pixel makes every run's file unique, so content-based dedupe never kicks in. */
    private static byte[] tinyPng() throws Exception {
        BufferedImage image = new BufferedImage(4, 4, BufferedImage.TYPE_INT_RGB);
        image.setRGB(0, 0, ThreadLocalRandom.current().nextInt(0xFFFFFF));
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            ImageIO.write(image, "png", out);
            return out.toByteArray();
        }
    }
}
