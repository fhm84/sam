package de.halbmann.sam.api.boundary;

import static io.restassured.RestAssured.given;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import de.halbmann.sam.api.entity.collections.CollectionItemType;
import de.halbmann.sam.api.entity.collections.CollectionVisibility;
import de.halbmann.sam.api.entity.collections.CreateCollectionItem;
import de.halbmann.sam.api.entity.collections.SheetCollection;
import de.halbmann.sam.api.entity.shared.PaginationRequest;
import de.halbmann.sam.business.collections.boundary.CollectionItemRepository;
import de.halbmann.sam.business.collections.boundary.SheetCollectionRepository;
import de.halbmann.sam.business.collections.controller.SheetCollectionService;
import de.halbmann.sam.business.collections.entity.TextCollectionItemEntity;
import io.quarkus.narayana.jta.QuarkusTransaction;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.security.TestSecurity;
import io.quarkus.test.security.oidc.Claim;
import io.quarkus.test.security.oidc.OidcSecurity;
import io.restassured.http.ContentType;
import jakarta.inject.Inject;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * The item endpoints act only on items of the collection in the URL: access is checked on that
 * collection, so another collection's item, here one in someone else's private draft, answers 404
 * and stays untouched.
 */
@QuarkusTest
@TestSecurity(
        user = "librarian2",
        roles = {"music_librarian"})
@OidcSecurity(claims = @Claim(key = "sub", value = "sub-librarian-b"))
class CollectionItemOwnershipTest {

    private static final String ITEMS = "/api/sheet-collections/{collectionId}/items/{itemId}";

    @Inject
    SheetCollectionService collectionService;

    @Inject
    SheetCollectionRepository collectionRepository;

    @Inject
    CollectionItemRepository itemRepository;

    private String open;
    private String openItem;
    private String draft;
    private String draftItem;
    private UUID draftAttachment;

    @BeforeEach
    void createCollections() throws Exception {
        open = createCollection("open", CollectionVisibility.WHOLE_ENSEMBLE, null);
        openItem = addTextItem(open, "open text");
        draft = createCollection("draft", CollectionVisibility.PRIVATE, "sub-librarian-a");
        draftItem = addTextItem(draft, "draft text");
        draftAttachment = collectionService
                .attachDocument(
                        draft,
                        draftItem,
                        "note.txt",
                        new ByteArrayInputStream(("note " + UUID.randomUUID()).getBytes(StandardCharsets.UTF_8)))
                .getId();
    }

    @AfterEach
    void deleteCollections() {
        collectionService.delete(open);
        collectionService.delete(draft);
    }

    private String createCollection(String name, CollectionVisibility visibility, String createdBy) {
        SheetCollection collection = new SheetCollection();
        collection.setName("Item ownership " + name + " " + UUID.randomUUID());
        collection.setVisibility(visibility);
        String id = collectionService.add(collection).getId().toString();
        QuarkusTransaction.requiringNew()
                .run(() -> collectionRepository.findById(UUID.fromString(id)).setCreatedBy(createdBy));
        return id;
    }

    private String addTextItem(String collectionId, String text) {
        CreateCollectionItem item = new CreateCollectionItem();
        item.setType(CollectionItemType.TEXT);
        item.setTextContent(text);
        collectionService.addItem(collectionId, item);
        return collectionService
                .listItems(collectionId, new PaginationRequest(), false, null)
                .getData()
                .getFirst()
                .getId()
                .toString();
    }

    private static String textUpdate(String text) {
        return "{\"type\":\"TEXT\",\"identifier\":\"x\",\"textContent\":\"" + text + "\"}";
    }

    @Test
    void anotherCollectionsItem_isNotFoundThroughAVisibleCollection() {
        given().contentType(ContentType.JSON)
                .body(textUpdate("changed"))
                .put(ITEMS, open, draftItem)
                .then()
                .statusCode(404);
        given().multiPart("file", "swap.txt", "swapped".getBytes(StandardCharsets.UTF_8), "text/plain")
                .post(ITEMS + "/attachment", open, draftItem)
                .then()
                .statusCode(404);
        given().delete(ITEMS + "/attachment", open, draftItem).then().statusCode(404);
        given().delete(ITEMS, open, draftItem).then().statusCode(404);

        QuarkusTransaction.requiringNew().run(() -> {
            var item = (TextCollectionItemEntity) itemRepository.findById(UUID.fromString(draftItem));
            assertNotNull(item, "the draft's item was deleted");
            assertEquals("draft text", item.getTextContent());
            assertNotNull(item.getAttachment(), "the draft item's attachment was removed");
            assertEquals(draftAttachment, item.getAttachment().getId());
            assertEquals(
                    1,
                    collectionRepository
                            .findById(UUID.fromString(draft))
                            .getItems()
                            .size());
        });
    }

    @Test
    void ownItem_canStillBeChangedAndRemoved() {
        given().contentType(ContentType.JSON)
                .body(textUpdate("changed"))
                .put(ITEMS, open, openItem)
                .then()
                .statusCode(204);
        given().multiPart("file", "note.txt", "note".getBytes(StandardCharsets.UTF_8), "text/plain")
                .post(ITEMS + "/attachment", open, openItem)
                .then()
                .statusCode(200);
        given().delete(ITEMS + "/attachment", open, openItem).then().statusCode(204);
        given().delete(ITEMS, open, openItem).then().statusCode(204);

        QuarkusTransaction.requiringNew()
                .run(() -> assertEquals(
                        0,
                        collectionRepository
                                .findById(UUID.fromString(open))
                                .getItems()
                                .size()));
    }
}
