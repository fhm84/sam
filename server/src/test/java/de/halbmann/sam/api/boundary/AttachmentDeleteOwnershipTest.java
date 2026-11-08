package de.halbmann.sam.api.boundary;

import static io.restassured.RestAssured.given;
import static org.junit.jupiter.api.Assertions.assertTrue;

import de.halbmann.sam.api.entity.collections.CollectionItemType;
import de.halbmann.sam.api.entity.collections.CollectionVisibility;
import de.halbmann.sam.api.entity.collections.CreateCollectionItem;
import de.halbmann.sam.api.entity.collections.SheetCollection;
import de.halbmann.sam.api.entity.shared.PaginationRequest;
import de.halbmann.sam.business.collections.boundary.SheetCollectionRepository;
import de.halbmann.sam.business.collections.controller.SheetCollectionService;
import de.halbmann.sam.business.documents.boundary.AttachmentRepository;
import de.halbmann.sam.business.sheets.controller.SheetService;
import io.quarkus.narayana.jta.QuarkusTransaction;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.security.TestSecurity;
import io.quarkus.test.security.oidc.Claim;
import io.quarkus.test.security.oidc.OidcSecurity;
import jakarta.inject.Inject;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Deleting a document through a sheet's (or instrumentation's) path only deletes attachments of
 * that sheet: another attachment, here a private draft's programme note, answers 404 and stays.
 */
@QuarkusTest
@TestSecurity(
        user = "librarian2",
        roles = {"music_librarian"})
@OidcSecurity(claims = @Claim(key = "sub", value = "sub-librarian-b"))
class AttachmentDeleteOwnershipTest {

    @Inject
    SheetService sheetService;

    @Inject
    SheetCollectionService collectionService;

    @Inject
    SheetCollectionRepository collectionRepository;

    @Inject
    AttachmentRepository attachmentRepository;

    private String sheetId;
    private String draft;
    private String draftAttachment;

    @BeforeEach
    void createData() throws Exception {
        sheetId = sheetService
                .addSheet(ObjectMother.createSimpleSheetMusic())
                .getId()
                .toString();
        SheetCollection collection = new SheetCollection();
        collection.setName("Delete ownership draft " + UUID.randomUUID());
        collection.setVisibility(CollectionVisibility.PRIVATE);
        draft = collectionService.add(collection).getId().toString();
        QuarkusTransaction.requiringNew()
                .run(() -> collectionRepository.findById(UUID.fromString(draft)).setCreatedBy("sub-librarian-a"));
        CreateCollectionItem item = new CreateCollectionItem();
        item.setType(CollectionItemType.TEXT);
        item.setTextContent("programme note");
        collectionService.addItem(draft, item);
        String itemId = collectionService
                .listItems(draft, new PaginationRequest(), false, null)
                .getData()
                .getFirst()
                .getId()
                .toString();
        draftAttachment = collectionService
                .attachDocument(
                        draft,
                        itemId,
                        "note.txt",
                        new ByteArrayInputStream(("note " + UUID.randomUUID()).getBytes(StandardCharsets.UTF_8)))
                .getId()
                .toString();
    }

    @AfterEach
    void deleteData() {
        sheetService.deleteSheet(sheetId);
        collectionService.delete(draft);
    }

    @Test
    void anotherAttachment_isNotFoundThroughASheet_andStays() {
        given().delete("/api/sheets/{sheetId}/documents/{id}", sheetId, draftAttachment)
                .then()
                .statusCode(404);

        assertTrue(QuarkusTransaction.requiringNew()
                .call(() -> attachmentRepository
                        .findByIdOptional(UUID.fromString(draftAttachment))
                        .isPresent()));
    }

    @Test
    void theSheetsOwnAttachment_canStillBeDeleted() {
        String own = given().multiPart(
                        "file", "score.txt", ("score " + UUID.randomUUID()).getBytes(StandardCharsets.UTF_8))
                .post("/api/sheets/{sheetId}/documents", sheetId)
                .then()
                .statusCode(200)
                .extract()
                .path("attachment.id");

        given().delete("/api/sheets/{sheetId}/documents/{id}", sheetId, own)
                .then()
                .statusCode(204);
    }
}
