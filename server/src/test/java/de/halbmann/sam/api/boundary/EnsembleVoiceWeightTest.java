package de.halbmann.sam.api.boundary;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.security.TestSecurity;
import io.restassured.http.ContentType;
import io.restassured.response.ValidatableResponse;
import io.restassured.specification.RequestSpecification;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * A voice's weight may be left out (the UI's weight field is optional): creating or updating such a
 * voice stores 1.0 instead of failing on the primitive entity column.
 */
@QuarkusTest
@TestSecurity(
        user = "librarian1",
        roles = {"music_librarian"})
class EnsembleVoiceWeightTest {

    private String ensemble;

    @BeforeEach
    void createEnsemble() {
        ensemble = json().body("{\"name\":\"Weights " + UUID.randomUUID() + "\"}")
                .post("/api/ensembles")
                .then()
                .statusCode(200)
                .extract()
                .path("id");
    }

    @AfterEach
    void deleteEnsemble() {
        given().delete("/api/ensembles/{id}", ensemble);
    }

    @Test
    void createWithoutWeight_storesOne() {
        String voice = addVoice("{\"label\":\"Tuba\",\"weight\":null}");

        voice(voice).body("weight", equalTo(1.0f));
    }

    @Test
    void createWithWeight_keepsIt() {
        String voice = addVoice("{\"label\":\"Flute\",\"weight\":0.5}");

        voice(voice).body("weight", equalTo(0.5f));
    }

    @Test
    void updateWithoutWeight_storesOne() {
        String voice = addVoice("{\"label\":\"Trumpet\",\"weight\":3}");

        json().body("{\"label\":\"Trumpet 1\"}")
                .put("/api/ensembles/{e}/voices/{v}", ensemble, voice)
                .then()
                .statusCode(204);

        voice(voice).body("label", equalTo("Trumpet 1")).body("weight", equalTo(1.0f));
    }

    private String addVoice(String body) {
        return json().body(body)
                .post("/api/ensembles/{e}/voices", ensemble)
                .then()
                .statusCode(200)
                .extract()
                .path("id");
    }

    private ValidatableResponse voice(String voice) {
        return given().get("/api/ensembles/{e}/voices/{v}", ensemble, voice)
                .then()
                .statusCode(200);
    }

    private static RequestSpecification json() {
        return given().contentType(ContentType.JSON);
    }
}
