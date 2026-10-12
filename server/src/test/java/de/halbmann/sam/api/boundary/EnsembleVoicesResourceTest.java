package de.halbmann.sam.api.boundary;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

import de.halbmann.sam.api.entity.ensembles.CreateEnsemble;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.security.TestSecurity;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Regression test for the ensemble voices sub-resource: the Angular voice form leaves
 * {@code weight} {@code null} when the user doesn't fill it in (no required validator), and that literal
 * JSON {@code null} used to NPE while Yasson tried to unbox it into what was a primitive
 * {@code double} field on {@link de.halbmann.sam.api.entity.ensembles.CreateEnsembleVoice} /
 * {@link de.halbmann.sam.api.entity.ensembles.EnsembleVoice}, returning a 500 instead of applying
 * a sensible default.
 */
@QuarkusTest
@TestSecurity(
        user = "librarian1",
        roles = {"music_librarian"})
class EnsembleVoicesResourceTest {

    private String ensembleId;

    @BeforeEach
    void setUp() {
        CreateEnsemble createEnsemble = new CreateEnsemble();
        createEnsemble.setName("Voices Test Ensemble");
        ensembleId = given().contentType(ContentType.JSON)
                .body(createEnsemble)
                .post("/api/ensembles")
                .then()
                .statusCode(200)
                .extract()
                .path("id");
    }

    @Test
    void addVoice_withNullWeight_defaultsInsteadOfFailing() {
        given().contentType(ContentType.JSON)
                .body("{\"label\":\"Horn 1\",\"weight\":null}")
                .post("/api/ensembles/{ensembleId}/voices", ensembleId)
                .then()
                .statusCode(200)
                .body("label", equalTo("Horn 1"))
                .body("weight", equalTo(1.0f));
    }

    @Test
    void addVoice_withExplicitWeight_keepsIt() {
        given().contentType(ContentType.JSON)
                .body("{\"label\":\"Horn 2\",\"weight\":2.5}")
                .post("/api/ensembles/{ensembleId}/voices", ensembleId)
                .then()
                .statusCode(200)
                .body("weight", equalTo(2.5f));
    }
}
