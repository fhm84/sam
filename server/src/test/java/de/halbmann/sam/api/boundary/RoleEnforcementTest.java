package de.halbmann.sam.api.boundary;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.oneOf;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.security.TestSecurity;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * Every endpoint with {@code @RolesAllowed}, called by a user who lacks the role, answers 403, and
 * a user who holds it gets past the role check (whatever the random IDs and empty body then cause:
 * 404, 400, and 500 on a few endpoints that don't validate the body yet).
 * A dropped {@code @RolesAllowed} fails here. Keep the lists in sync with the {@code *ResourceImpl}
 * classes and the "Read access by role" table in {@code docs/features/api-surface.md}.
 */
@QuarkusTest
class RoleEnforcementTest {

    private static final String ID = "00000000-0000-0000-0000-000000000001";
    private static final String SHEET = "/api/sheets/" + ID;
    private static final String COLLECTION = "/api/sheet-collections/" + ID;
    private static final String ENSEMBLE = "/api/ensembles/" + ID;

    /** music_librarian or admin. */
    static Stream<String> writeEndpoints() {
        return Stream.of(
                "POST /api/sheets",
                "PUT " + SHEET,
                "DELETE " + SHEET,
                "POST " + SHEET + "/tags",
                "DELETE " + SHEET + "/tags",
                "POST " + SHEET + "/enrich",
                "POST " + SHEET + "/instrumentations",
                "POST " + SHEET + "/instrumentations/bulk",
                "PUT " + SHEET + "/instrumentations/" + ID,
                "DELETE " + SHEET + "/instrumentations/" + ID,
                "GET /api/documents/unlinked",
                "POST /api/documents/" + ID + "/link",
                "POST /api/documents/" + ID + "/classify",
                "POST /api/documents/" + ID + "/apply",
                "DELETE /api/documents/" + ID,
                "POST /api/sheet-collections",
                "PUT " + COLLECTION,
                "DELETE " + COLLECTION,
                "GET " + COLLECTION + "/gema-setlist",
                "POST " + COLLECTION + "/ai/suggest-items",
                "POST " + COLLECTION + "/items",
                "PUT " + COLLECTION + "/items/" + ID,
                "DELETE " + COLLECTION + "/items/" + ID,
                "DELETE " + COLLECTION + "/items/" + ID + "/attachment",
                "PUT " + COLLECTION + "/items/order",
                "POST " + COLLECTION + "/items/" + ID + "/ai/draft-text",
                "POST /api/ensembles",
                "PUT " + ENSEMBLE,
                "DELETE " + ENSEMBLE,
                "POST " + ENSEMBLE + "/coverage/compute",
                "POST " + ENSEMBLE + "/members",
                "PUT " + ENSEMBLE + "/members/" + ID,
                "DELETE " + ENSEMBLE + "/members/" + ID,
                "POST " + ENSEMBLE + "/voices",
                "PUT " + ENSEMBLE + "/voices/" + ID,
                "DELETE " + ENSEMBLE + "/voices/" + ID,
                "POST " + ENSEMBLE + "/voices/" + ID + "/options",
                "PUT " + ENSEMBLE + "/voices/" + ID + "/options/" + ID,
                "DELETE " + ENSEMBLE + "/voices/" + ID + "/options/" + ID,
                "POST /api/instruments",
                "PUT /api/instruments/FLUTE_C",
                "DELETE /api/instruments/FLUTE_C",
                "POST /api/persons",
                "PUT /api/persons/" + ID,
                "DELETE /api/persons/" + ID,
                "POST /api/persons/" + ID + "/merge-into/" + ID,
                "POST /api/members",
                "PUT /api/members/" + ID,
                "DELETE /api/members/" + ID,
                "POST /api/shares");
    }

    /** admin only. */
    static Stream<String> adminEndpoints() {
        return Stream.of(
                "GET /api/event-logs",
                "GET /api/admin/users",
                "GET /api/admin/users/directory",
                "GET /api/admin/users/some-user",
                "PUT /api/members/" + ID + "/user/some-user",
                "DELETE /api/members/" + ID + "/user");
    }

    private static Response call(String endpoint) {
        String[] parts = endpoint.split(" ", 2);
        // An empty JSON body: the role check must answer before the body matters
        return given().contentType(ContentType.JSON).body("{}").request(parts[0], parts[1]);
    }

    @ParameterizedTest
    @MethodSource({"writeEndpoints", "adminEndpoints"})
    @TestSecurity(
            user = "musician1",
            roles = {})
    void plainUser_isForbidden(String endpoint) {
        call(endpoint).then().statusCode(403);
    }

    @ParameterizedTest
    @MethodSource("adminEndpoints")
    @TestSecurity(
            user = "librarian1",
            roles = {"music_librarian"})
    void librarian_isForbiddenFromAdminEndpoints(String endpoint) {
        call(endpoint).then().statusCode(403);
    }

    @ParameterizedTest
    @MethodSource("writeEndpoints")
    @TestSecurity(
            user = "librarian1",
            roles = {"music_librarian"})
    void librarian_passesTheRoleCheck(String endpoint) {
        call(endpoint).then().statusCode(not(oneOf(401, 403)));
    }
}
