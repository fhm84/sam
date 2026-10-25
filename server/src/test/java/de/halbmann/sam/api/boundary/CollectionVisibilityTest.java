package de.halbmann.sam.api.boundary;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertEquals;

import de.halbmann.sam.api.entity.collections.CollectionItemType;
import de.halbmann.sam.api.entity.collections.CollectionVisibility;
import de.halbmann.sam.api.entity.collections.CreateCollectionItem;
import de.halbmann.sam.api.entity.collections.SheetCollection;
import de.halbmann.sam.api.entity.sheets.CreateSheetMusic;
import de.halbmann.sam.business.collections.boundary.SheetCollectionRepository;
import de.halbmann.sam.business.collections.controller.SheetCollectionService;
import de.halbmann.sam.business.sheets.controller.SheetService;
import io.quarkus.narayana.jta.QuarkusTransaction;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.security.TestSecurity;
import io.quarkus.test.security.oidc.Claim;
import io.quarkus.test.security.oidc.OidcSecurity;
import io.restassured.http.ContentType;
import jakarta.inject.Inject;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Who sees which collection: {@code WHOLE_ENSEMBLE} and unset every login; {@code ADMINS_ONLY} the
 * music librarian and the admin; {@code PRIVATE} its creator and the admin, and every librarian
 * when no creator was recorded. A hidden collection answers 404, for reads and writes alike. Test
 * data is created through the services, independent of the caller.
 */
@QuarkusTest
class CollectionVisibilityTest {

    private static final String CREATOR = "sub-librarian-a";

    @Inject
    SheetCollectionService collectionService;

    @Inject
    SheetCollectionRepository collectionRepository;

    @Inject
    SheetService sheetService;

    private final String tag = "Visibility " + UUID.randomUUID();
    private String open;
    private String unset;
    private String adminsOnly;
    private String draft;
    private String legacyPrivate;
    private String sheetId;

    @BeforeEach
    void createCollections() {
        open = createCollection("open", CollectionVisibility.WHOLE_ENSEMBLE, null);
        unset = createCollection("unset", null, null);
        adminsOnly = createCollection("admins", CollectionVisibility.ADMINS_ONLY, null);
        draft = createCollection("draft", CollectionVisibility.PRIVATE, CREATOR);
        legacyPrivate = createCollection("legacy private", CollectionVisibility.PRIVATE, null);
        // One sheet in every collection, for the sheet's collections endpoint
        CreateSheetMusic sheet = ObjectMother.createSimpleSheetMusic();
        sheet.setTitle(tag);
        sheetId = sheetService.addSheet(sheet).getId().toString();
        for (String collection : all()) {
            CreateCollectionItem item = new CreateCollectionItem();
            item.setType(CollectionItemType.SHEET);
            item.setSheetId(UUID.fromString(sheetId));
            collectionService.addItem(collection, item);
        }
    }

    @AfterEach
    void deleteData() {
        sheetService.deleteSheet(sheetId);
        for (String id : all()) {
            collectionService.delete(id);
        }
        // Collections created through the API by a test
        QuarkusTransaction.requiringNew().run(() -> collectionRepository.delete("name like ?1", tag + "%"));
    }

    private String[] all() {
        return new String[] {open, unset, adminsOnly, draft, legacyPrivate};
    }

    private String createCollection(String name, CollectionVisibility visibility, String createdBy) {
        SheetCollection collection = new SheetCollection();
        collection.setName(tag + " " + name);
        collection.setVisibility(visibility);
        String id = collectionService.add(collection).getId().toString();
        QuarkusTransaction.requiringNew()
                .run(() -> collectionRepository.findById(UUID.fromString(id)).setCreatedBy(createdBy));
        return id;
    }

    private static void listsExactly(String path, String tag, String[] visible, String... hidden) {
        var response = given().queryParam("name", tag).get(path).then().statusCode(200);
        response.body("data.id", hasItems(visible));
        for (String id : hidden) {
            response.body("data.id", not(hasItem(id)));
        }
    }

    @Test
    @TestSecurity(
            user = "musician1",
            roles = {})
    void plainUser_seesOnlyOpenCollections() {
        String[] visible = {open, unset};
        listsExactly("/api/sheet-collections", tag, visible, adminsOnly, draft, legacyPrivate);
        listsExactly("/api/sheets/" + sheetId + "/collections", tag, visible, adminsOnly, draft, legacyPrivate);
        for (String id : new String[] {adminsOnly, draft, legacyPrivate}) {
            given().get("/api/sheet-collections/" + id).then().statusCode(404);
            given().get("/api/sheet-collections/" + id + "/items").then().statusCode(404);
            given().get("/api/sheet-collections/" + id + "/toc").then().statusCode(404);
            given().get("/api/sheet-collections/" + id + "/export").then().statusCode(404);
        }
        given().get("/api/sheet-collections/" + open).then().statusCode(200);
        given().get("/api/sheet-collections/" + unset + "/items").then().statusCode(200);
    }

    @Test
    @TestSecurity(
            user = "librarian1",
            roles = {"music_librarian"})
    @OidcSecurity(claims = @Claim(key = "sub", value = CREATOR))
    void creator_seesTheirDraft() {
        String[] visible = {open, unset, adminsOnly, draft, legacyPrivate};
        listsExactly("/api/sheet-collections", tag, visible);
        listsExactly("/api/sheets/" + sheetId + "/collections", tag, visible);
        given().get("/api/sheet-collections/" + draft + "/items").then().statusCode(200);
    }

    @Test
    @TestSecurity(
            user = "librarian2",
            roles = {"music_librarian"})
    @OidcSecurity(claims = @Claim(key = "sub", value = "sub-librarian-b"))
    void otherLibrarian_neitherSeesNorChangesSomeoneElsesDraft() {
        String[] visible = {open, unset, adminsOnly, legacyPrivate};
        listsExactly("/api/sheet-collections", tag, visible, draft);
        listsExactly("/api/sheets/" + sheetId + "/collections", tag, visible, draft);
        given().get("/api/sheet-collections/" + draft).then().statusCode(404);
        given().contentType(ContentType.JSON)
                .body("{\"name\":\"taken over\"}")
                .put("/api/sheet-collections/" + draft)
                .then()
                .statusCode(404);
        given().contentType(ContentType.JSON)
                .body("{\"type\":\"TEXT\",\"title\":\"x\"}")
                .post("/api/sheet-collections/" + draft + "/items")
                .then()
                .statusCode(404);
        given().contentType(ContentType.JSON)
                .body("{\"resourceType\":\"COLLECTION\",\"resourceId\":\"" + draft + "\"}")
                .post("/api/shares")
                .then()
                .statusCode(404);
        given().delete("/api/sheet-collections/" + draft).then().statusCode(404);
        // A private collection without a recorded creator stays visible to librarians
        given().get("/api/sheet-collections/" + legacyPrivate).then().statusCode(200);
    }

    @Test
    @TestSecurity(
            user = "admin1",
            roles = {"admin"})
    @OidcSecurity(claims = @Claim(key = "sub", value = "sub-admin"))
    void admin_seesEveryCollection() {
        listsExactly("/api/sheet-collections", tag, all());
        given().get("/api/sheet-collections/" + draft).then().statusCode(200);
    }

    @Test
    @TestSecurity(
            user = "librarian1",
            roles = {"music_librarian"})
    @OidcSecurity(claims = @Claim(key = "sub", value = CREATOR))
    void creatingACollection_recordsTheCreator() {
        String id = given().contentType(ContentType.JSON)
                .body("{\"name\":\"" + tag + " created\",\"visibility\":\"PRIVATE\"}")
                .post("/api/sheet-collections")
                .then()
                .statusCode(200)
                .extract()
                .path("id");
        String createdBy = QuarkusTransaction.requiringNew()
                .call(() -> collectionRepository.findById(UUID.fromString(id)).getCreatedBy());
        assertEquals(CREATOR, createdBy);
    }
}
