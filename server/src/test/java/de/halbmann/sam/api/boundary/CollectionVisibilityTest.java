package de.halbmann.sam.api.boundary;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.not;

import de.halbmann.sam.api.entity.collections.CollectionItemType;
import de.halbmann.sam.api.entity.collections.CollectionVisibility;
import de.halbmann.sam.api.entity.collections.CreateCollectionItem;
import de.halbmann.sam.api.entity.collections.SheetCollection;
import de.halbmann.sam.api.entity.sheets.CreateSheetMusic;
import de.halbmann.sam.business.collections.controller.SheetCollectionService;
import de.halbmann.sam.business.sheets.controller.SheetService;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.security.TestSecurity;
import jakarta.inject.Inject;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Collections marked {@code ADMINS_ONLY} or {@code PRIVATE} are visible only to the music librarian
 * and the admin; {@code WHOLE_ENSEMBLE} and unset ones to every login. A hidden collection answers
 * 404, as if it didn't exist. Test data is created through the services, independent of the caller.
 */
@QuarkusTest
class CollectionVisibilityTest {

    @Inject
    SheetCollectionService collectionService;

    @Inject
    SheetService sheetService;

    private final String tag = "Visibility " + UUID.randomUUID();
    private String open;
    private String unset;
    private String adminsOnly;
    private String privateOne;
    private String sheetId;

    @BeforeEach
    void createCollections() {
        open = createCollection("open", CollectionVisibility.WHOLE_ENSEMBLE);
        unset = createCollection("unset", null);
        adminsOnly = createCollection("admins", CollectionVisibility.ADMINS_ONLY);
        privateOne = createCollection("private", CollectionVisibility.PRIVATE);
        // One sheet in all four collections, for the sheet's collections endpoint
        CreateSheetMusic sheet = ObjectMother.createSimpleSheetMusic();
        sheet.setTitle(tag);
        sheet.setCollectionId(UUID.fromString(open));
        sheetId = sheetService.addSheet(sheet).getId().toString();
        for (String collection : new String[] {unset, adminsOnly, privateOne}) {
            CreateCollectionItem item = new CreateCollectionItem();
            item.setType(CollectionItemType.SHEET);
            item.setSheetId(UUID.fromString(sheetId));
            collectionService.addItem(collection, item);
        }
    }

    @AfterEach
    void deleteData() {
        sheetService.deleteSheet(sheetId);
        for (String id : new String[] {open, unset, adminsOnly, privateOne}) {
            collectionService.delete(id);
        }
    }

    private String createCollection(String name, CollectionVisibility visibility) {
        SheetCollection collection = new SheetCollection();
        collection.setName(tag + " " + name);
        collection.setVisibility(visibility);
        return collectionService.add(collection).getId().toString();
    }

    @Test
    @TestSecurity(
            user = "musician1",
            roles = {})
    void plainUser_listsOnlyOpenCollections() {
        given().queryParam("name", tag)
                .get("/api/sheet-collections")
                .then()
                .statusCode(200)
                .body("data.id", hasItems(open, unset))
                .body("data.id", not(hasItem(adminsOnly)))
                .body("data.id", not(hasItem(privateOne)));
    }

    @Test
    @TestSecurity(
            user = "musician1",
            roles = {})
    void plainUser_seesOnlyOpenCollectionsOfASheet() {
        given().get("/api/sheets/" + sheetId + "/collections")
                .then()
                .statusCode(200)
                .body("data.id", hasItems(open, unset))
                .body("data.id", not(hasItem(adminsOnly)))
                .body("data.id", not(hasItem(privateOne)));
    }

    @Test
    @TestSecurity(
            user = "musician1",
            roles = {})
    void plainUser_getsNotFoundForRestrictedCollections() {
        for (String id : new String[] {adminsOnly, privateOne}) {
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
    void librarian_seesEveryCollection() {
        given().queryParam("name", tag)
                .get("/api/sheet-collections")
                .then()
                .statusCode(200)
                .body("data.id", hasItems(open, unset, adminsOnly, privateOne));
        given().get("/api/sheets/" + sheetId + "/collections")
                .then()
                .statusCode(200)
                .body("data.id", hasItems(open, unset, adminsOnly, privateOne));
        given().get("/api/sheet-collections/" + privateOne + "/items").then().statusCode(200);
    }

    @Test
    @TestSecurity(
            user = "admin1",
            roles = {"admin"})
    void admin_loadsARestrictedCollection() {
        given().get("/api/sheet-collections/" + adminsOnly).then().statusCode(200);
    }
}
