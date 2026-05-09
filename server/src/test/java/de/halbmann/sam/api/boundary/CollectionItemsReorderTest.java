package de.halbmann.sam.api.boundary;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertEquals;

import de.halbmann.sam.api.entity.collections.CollectionItemType;
import de.halbmann.sam.api.entity.collections.CollectionType;
import de.halbmann.sam.api.entity.collections.CreateCollectionItem;
import de.halbmann.sam.api.entity.collections.SheetCollection;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.security.TestSecurity;
import io.restassured.http.ContentType;
import jakarta.json.bind.Jsonb;
import jakarta.json.bind.JsonbBuilder;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Verifies that collection item reordering persists correctly and does not trigger duplicate
 * items_order constraint violations during the clear-flush-re-insert cycle.
 */
@QuarkusTest
@TestSecurity(
        user = "librarian1",
        roles = {"music_librarian"})
class CollectionItemsReorderTest {

    private String collectionId;
    private List<String> itemIds;

    @BeforeEach
    void setUp() throws Exception {
        try (Jsonb jsonb = JsonbBuilder.newBuilder().build()) {
            SheetCollection collection = new SheetCollection();
            collection.setName("Reorder Test Setlist");
            collection.setType(CollectionType.SETLIST);

            collectionId = given().contentType(ContentType.JSON)
                    .body(jsonb.toJson(collection))
                    .post("/api/sheet-collections")
                    .then()
                    .statusCode(200)
                    .extract()
                    .path("id");

            for (String text : List.of("Item A", "Item B", "Item C")) {
                CreateCollectionItem item = new CreateCollectionItem();
                item.setType(CollectionItemType.TEXT);
                item.setTextContent(text);
                given().contentType(ContentType.JSON)
                        .body(jsonb.toJson(item))
                        .post("/api/sheet-collections/{id}/items", collectionId)
                        .then()
                        .statusCode(204);
            }

            itemIds = given().get("/api/sheet-collections/{id}/items", collectionId)
                    .then()
                    .statusCode(200)
                    .extract()
                    .jsonPath()
                    .getList("data.id", String.class);
        }
    }

    @Test
    void reorderItems_persistsNewOrder() throws Exception {
        assertEquals(3, itemIds.size());

        // Reverse the order: C, B, A
        List<String> reversed = List.of(itemIds.get(2), itemIds.get(1), itemIds.get(0));

        try (Jsonb jsonb = JsonbBuilder.newBuilder().build()) {
            given().contentType(ContentType.JSON)
                    .body(jsonb.toJson(reversed))
                    .put("/api/sheet-collections/{id}/items/order", collectionId)
                    .then()
                    .statusCode(204);
        }

        List<String> reorderedTexts = given().get("/api/sheet-collections/{id}/items", collectionId)
                .then()
                .statusCode(200)
                .extract()
                .jsonPath()
                .getList("data.textContent", String.class);

        assertEquals(List.of("Item C", "Item B", "Item A"), reorderedTexts);
    }

    @Test
    void reorderItems_noConstraintViolationOnAdjacentSwap() throws Exception {
        // Swap just the first two items — this is the case most likely to hit a transient
        // duplicate items_order if flush() is missing between clear() and addAll().
        List<String> swapped = List.of(itemIds.get(1), itemIds.get(0), itemIds.get(2));

        try (Jsonb jsonb = JsonbBuilder.newBuilder().build()) {
            given().contentType(ContentType.JSON)
                    .body(jsonb.toJson(swapped))
                    .put("/api/sheet-collections/{id}/items/order", collectionId)
                    .then()
                    .statusCode(204);
        }

        List<String> texts = given().get("/api/sheet-collections/{id}/items", collectionId)
                .then()
                .statusCode(200)
                .extract()
                .jsonPath()
                .getList("data.textContent", String.class);

        assertEquals(List.of("Item B", "Item A", "Item C"), texts);
    }
}
