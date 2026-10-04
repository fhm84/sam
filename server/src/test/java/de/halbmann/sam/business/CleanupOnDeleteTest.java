package de.halbmann.sam.business;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.*;

import de.halbmann.sam.api.entity.musicians.Musician;
import de.halbmann.sam.api.entity.shares.ShareType;
import de.halbmann.sam.api.entity.sheets.SheetMusic;
import de.halbmann.sam.business.collections.boundary.CollectionItemRepository;
import de.halbmann.sam.business.collections.entity.TextCollectionItemEntity;
import de.halbmann.sam.business.documents.boundary.DocumentRepository;
import de.halbmann.sam.business.shares.boundary.ShareRepository;
import de.halbmann.sam.business.shares.entity.ShareEntity;
import io.quarkus.narayana.jta.QuarkusTransaction;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.security.TestSecurity;
import io.restassured.http.ContentType;
import jakarta.inject.Inject;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/**
 * Deleting things must not leave debris behind or fail on foreign keys: collection items and
 * their attachments go with the collection, musicians still in use are refused with 409 instead of
 * a constraint-violation 500, and share links to a deleted resource are revoked.
 */
@QuarkusTest
@TestSecurity(
        user = "librarian1",
        roles = {"music_librarian"})
class CleanupOnDeleteTest {

    @Inject
    CollectionItemRepository collectionItemRepository;

    @Inject
    DocumentRepository documentRepository;

    @Inject
    ShareRepository shareRepository;

    @Test
    void deleteCollection_removesItemsAndReleasesTextItemAttachments() {
        String collectionId = given().contentType(ContentType.JSON)
                .body("{\"name\":\"Concert " + UUID.randomUUID() + "\",\"type\":\"SETLIST\"}")
                .post("/api/sheet-collections")
                .then()
                .statusCode(200)
                .extract()
                .path("id");
        given().contentType(ContentType.JSON)
                .body("{\"type\":\"TEXT\",\"textContent\":\"Welcome\"}")
                .post("/api/sheet-collections/{id}/items", collectionId)
                .then()
                .statusCode(204);
        String itemId = given().get("/api/sheet-collections/{id}/items", collectionId)
                .then()
                .statusCode(200)
                .extract()
                .path("data[0].id");
        byte[] note = ("programme note " + UUID.randomUUID()).getBytes(StandardCharsets.UTF_8);
        given().multiPart("file", "note.txt", note, "text/plain")
                .post("/api/sheet-collections/{id}/items/{itemId}/attachment", collectionId, itemId)
                .then()
                .statusCode(200);
        UUID documentId = QuarkusTransaction.requiringNew().call(() -> {
            var item = (TextCollectionItemEntity) collectionItemRepository.findById(UUID.fromString(itemId));
            return item.getAttachment().getDocument().getId();
        });

        given().delete("/api/sheet-collections/{id}", collectionId).then().statusCode(204);

        QuarkusTransaction.requiringNew().run(() -> {
            assertNull(collectionItemRepository.findById(UUID.fromString(itemId)), "collection item left behind");
            var document = documentRepository.findById(documentId);
            assertTrue(document == null || document.getRefCount() == 0, "attachment still holds a reference");
        });
    }

    @Test
    void deleteMusician_stillUsedAsComposer_returns409() {
        String composerName = "Busy Composer " + UUID.randomUUID();
        String sheetId = createSheet(composerName);
        String composerId =
                given().get("/api/sheets/{id}", sheetId).then().extract().path("composer.id");

        given().delete("/api/musicians/{id}", composerId).then().statusCode(409);

        given().delete("/api/sheets/{id}", sheetId).then().statusCode(204);
        given().delete("/api/musicians/{id}", composerId).then().statusCode(204);
    }

    @Test
    void deleteSheet_revokesItsShareLinks() {
        String sheetId = createSheet(null);
        UUID shareId = QuarkusTransaction.requiringNew().call(() -> {
            ShareEntity share = new ShareEntity();
            share.setCreatorUserId("librarian1");
            share.setResourceType(ShareType.SHEET);
            share.setResourceId(UUID.fromString(sheetId));
            shareRepository.persist(share);
            return share.getId();
        });

        given().delete("/api/sheets/{id}", sheetId).then().statusCode(204);

        QuarkusTransaction.requiringNew()
                .run(() -> assertNotNull(shareRepository.findById(shareId).getRevokedAt(), "share still active"));
        given().get("/api/public/shares/{token}", shareId)
                .then()
                .statusCode(200)
                .body("expired", is(true));
    }

    private String createSheet(String composerName) {
        SheetMusic sheet = new SheetMusic();
        sheet.setTitle("Sheet " + UUID.randomUUID());
        if (composerName != null) {
            Musician composer = new Musician();
            composer.setName(composerName);
            sheet.setComposer(composer);
        }
        return given().contentType(ContentType.JSON)
                .body(sheet)
                .post("/api/sheets")
                .then()
                .statusCode(200)
                .extract()
                .path("id");
    }
}
