package de.halbmann.sam.api.boundary;

import static io.restassured.RestAssured.given;
import static org.junit.jupiter.api.Assertions.assertEquals;

import de.halbmann.sam.api.entity.collections.CollectionItemType;
import de.halbmann.sam.api.entity.collections.CollectionVisibility;
import de.halbmann.sam.api.entity.collections.CreateCollectionItem;
import de.halbmann.sam.api.entity.collections.SheetCollection;
import de.halbmann.sam.api.entity.documents.AttachmentType;
import de.halbmann.sam.api.entity.shared.PaginationRequest;
import de.halbmann.sam.business.collections.boundary.SheetCollectionRepository;
import de.halbmann.sam.business.collections.controller.SheetCollectionService;
import de.halbmann.sam.business.documents.controller.AttachmentLinkService;
import de.halbmann.sam.business.documents.controller.DocumentStore;
import io.quarkus.narayana.jta.QuarkusTransaction;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.security.TestSecurity;
import io.quarkus.test.security.oidc.Claim;
import io.quarkus.test.security.oidc.OidcSecurity;
import io.restassured.http.ContentType;
import jakarta.inject.Inject;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import java.util.zip.ZipInputStream;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Reading attachments by their ID ({@code /documents/{id}}, {@code /documents/batch}) follows the
 * visibility of the collection whose text item carries them; other attachments stay open to every
 * login. A bare document ID (the upload inbox) is for librarians and admins.
 */
@QuarkusTest
class AttachmentVisibilityTest {

    private static final String CREATOR = "sub-librarian-a";

    @Inject
    SheetCollectionService collectionService;

    @Inject
    SheetCollectionRepository collectionRepository;

    @Inject
    DocumentStore documentStore;

    @Inject
    AttachmentLinkService attachmentLinkService;

    private String open;
    private String draft;
    private String openAttachment;
    private String draftAttachment;
    private String otherAttachment;
    private UUID bareDocument;

    @BeforeEach
    void createData() throws Exception {
        open = createCollection("open", CollectionVisibility.WHOLE_ENSEMBLE, null);
        openAttachment = addTextItemWithAttachment(open);
        draft = createCollection("draft", CollectionVisibility.PRIVATE, CREATOR);
        draftAttachment = addTextItemWithAttachment(draft);
        otherAttachment = documentStore
                .save("score.txt", content(), AttachmentType.FULL_SCORE)
                .attachment()
                .getId()
                .toString();
        bareDocument =
                documentStore.save("inbox.txt", content(), null).document().id();
    }

    @AfterEach
    void deleteData() {
        collectionService.delete(open);
        collectionService.delete(draft);
        attachmentLinkService.deleteAttachment(otherAttachment);
        documentStore.deleteIfUnlinked(bareDocument);
    }

    private static ByteArrayInputStream content() {
        return new ByteArrayInputStream(("file " + UUID.randomUUID()).getBytes(StandardCharsets.UTF_8));
    }

    private String createCollection(String name, CollectionVisibility visibility, String createdBy) {
        SheetCollection collection = new SheetCollection();
        collection.setName("Attachment visibility " + name + " " + UUID.randomUUID());
        collection.setVisibility(visibility);
        String id = collectionService.add(collection).getId().toString();
        QuarkusTransaction.requiringNew()
                .run(() -> collectionRepository.findById(UUID.fromString(id)).setCreatedBy(createdBy));
        return id;
    }

    private String addTextItemWithAttachment(String collectionId) throws Exception {
        CreateCollectionItem item = new CreateCollectionItem();
        item.setType(CollectionItemType.TEXT);
        item.setTextContent("programme note");
        collectionService.addItem(collectionId, item);
        String itemId = collectionService
                .listItems(collectionId, new PaginationRequest(), false, null)
                .getData()
                .getFirst()
                .getId()
                .toString();
        return collectionService
                .attachDocument(collectionId, itemId, "note.txt", content())
                .getId()
                .toString();
    }

    private static int get(String id) {
        return given().get("/api/documents/{id}", id).then().extract().statusCode();
    }

    /** Number of files in the batch ZIP, 0 for 204 No Content. */
    private static int batchFiles(String... ids) throws IOException {
        var response = given().contentType(ContentType.JSON)
                .body("{\"format\":\"ZIP\",\"ids\":[\"" + String.join("\",\"", ids) + "\"]}")
                .post("/api/documents/batch")
                .then()
                .extract();
        if (response.statusCode() == 204) {
            return 0;
        }
        assertEquals(200, response.statusCode());
        int files = 0;
        try (ZipInputStream zip = new ZipInputStream(response.asInputStream())) {
            while (zip.getNextEntry() != null) {
                files++;
            }
        }
        return files;
    }

    @Test
    @TestSecurity(
            user = "musician1",
            roles = {})
    void musician_readsOnlyAttachmentsOfCollectionsTheySee() throws IOException {
        assertEquals(404, get(draftAttachment));
        assertEquals(200, get(openAttachment));
        assertEquals(200, get(otherAttachment));
        assertEquals(404, get(bareDocument.toString()));
        assertEquals(0, batchFiles(draftAttachment));
        assertEquals(2, batchFiles(draftAttachment, openAttachment, otherAttachment));
    }

    @Test
    @TestSecurity(
            user = "librarian2",
            roles = {"music_librarian"})
    @OidcSecurity(claims = @Claim(key = "sub", value = "sub-librarian-b"))
    void otherLibrarian_doesNotReadSomeoneElsesDraft_butTheInbox() throws IOException {
        assertEquals(404, get(draftAttachment));
        assertEquals(0, batchFiles(draftAttachment));
        assertEquals(200, get(bareDocument.toString()));
    }

    @Test
    @TestSecurity(
            user = "librarian1",
            roles = {"music_librarian"})
    @OidcSecurity(claims = @Claim(key = "sub", value = CREATOR))
    void creator_readsTheirDraftsAttachment() throws IOException {
        assertEquals(200, get(draftAttachment));
        assertEquals(1, batchFiles(draftAttachment));
    }

    @Test
    @TestSecurity(
            user = "admin1",
            roles = {"admin"})
    @OidcSecurity(claims = @Claim(key = "sub", value = "sub-admin"))
    void admin_readsEverything() throws IOException {
        assertEquals(200, get(draftAttachment));
        assertEquals(200, get(bareDocument.toString()));
        assertEquals(3, batchFiles(draftAttachment, openAttachment, otherAttachment));
    }
}
