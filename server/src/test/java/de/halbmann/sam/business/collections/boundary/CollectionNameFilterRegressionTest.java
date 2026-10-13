package de.halbmann.sam.business.collections.boundary;

import static io.restassured.RestAssured.given;
import static org.junit.jupiter.api.Assertions.*;

import de.halbmann.sam.api.entity.collections.SheetCollection;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.security.TestSecurity;
import io.restassured.http.ContentType;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

/**
 * The collection {@code name} filter is a literal substring match. Regression test: the value went
 * into {@code LIKE} unescaped, so {@code %} or {@code _} acted as wildcards and matched every
 * collection.
 */
@QuarkusTest
@TestSecurity(
        user = "librarian1",
        roles = {"music_librarian"})
class CollectionNameFilterRegressionTest {

    private final List<String> createdIds = new ArrayList<>();

    @AfterEach
    void deleteCreatedData() {
        createdIds.forEach(id -> given().delete("/api/sheet-collections/{id}", id));
        createdIds.clear();
    }

    @Test
    void nameFilter_treatsWildcardCharactersLiterally() {
        String suffix = UUID.randomUUID().toString();
        String percent = createCollection("100% Brass " + suffix);
        String underscore = createCollection("Brass_Night " + suffix);
        String plain = createCollection("Plain Concert " + suffix);

        List<String> percentHits = searchIds("%");
        assertTrue(percentHits.contains(percent));
        assertFalse(percentHits.contains(underscore));
        assertFalse(percentHits.contains(plain));

        List<String> underscoreHits = searchIds("_");
        assertTrue(underscoreHits.contains(underscore));
        assertFalse(underscoreHits.contains(percent));
        assertFalse(underscoreHits.contains(plain));
    }

    private List<String> searchIds(String name) {
        return given().queryParam("name", name)
                .queryParam("size", -1)
                .get("/api/sheet-collections")
                .then()
                .statusCode(200)
                .extract()
                .jsonPath()
                .getList("data.id", String.class);
    }

    private String createCollection(String name) {
        SheetCollection collection = new SheetCollection();
        collection.setName(name);
        String id = given().contentType(ContentType.JSON)
                .body(collection)
                .post("/api/sheet-collections")
                .then()
                .statusCode(200)
                .extract()
                .path("id");
        createdIds.add(id);
        return id;
    }
}
