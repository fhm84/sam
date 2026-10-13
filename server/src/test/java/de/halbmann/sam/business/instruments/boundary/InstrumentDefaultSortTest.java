package de.halbmann.sam.business.instruments.boundary;

import static io.restassured.RestAssured.given;
import static org.junit.jupiter.api.Assertions.assertEquals;

import de.halbmann.sam.api.entity.instruments.CreateInstrument;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.security.TestSecurity;
import io.restassured.http.ContentType;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

/**
 * Without {@code sortBy}, instruments come back sorted by name. Regression test: JAX-RS binds an
 * absent {@code sortBy} to an empty array, which used to bypass the default sort entirely, so
 * e.g. the Sheets instrument filter dropdown listed instruments in table order.
 */
@QuarkusTest
@TestSecurity(
        user = "librarian1",
        roles = {"music_librarian"})
class InstrumentDefaultSortTest {

    private final List<String> createdIds = new ArrayList<>();

    @AfterEach
    void deleteCreatedData() {
        createdIds.forEach(id -> given().delete("/api/instruments/{id}", id));
        createdIds.clear();
    }

    @Test
    void withoutSortBy_instrumentsAreSortedByName() {
        String suffix = UUID.randomUUID().toString().replace("-", "_").toUpperCase(java.util.Locale.ROOT);
        // Created in reverse order, so table order differs from name order
        String zzz = createInstrument("ZZZ_" + suffix, "Zzz sort test " + suffix);
        String mmm = createInstrument("MMM_" + suffix, "Mmm sort test " + suffix);
        String aaa = createInstrument("AAA_" + suffix, "Aaa sort test " + suffix);

        List<String> ids = given().queryParam("size", -1)
                .get("/api/instruments")
                .then()
                .statusCode(200)
                .extract()
                .jsonPath()
                .getList("data.id", String.class);

        assertEquals(
                List.of(aaa, mmm, zzz),
                ids.stream().filter(createdIds::contains).toList());
    }

    private String createInstrument(String id, String name) {
        CreateInstrument instrument = new CreateInstrument();
        instrument.setId(id);
        instrument.setName(name);
        given().contentType(ContentType.JSON)
                .body(instrument)
                .post("/api/instruments")
                .then()
                .statusCode(200);
        createdIds.add(id);
        return id;
    }
}
