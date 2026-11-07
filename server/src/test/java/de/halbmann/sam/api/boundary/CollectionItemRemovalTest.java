package de.halbmann.sam.api.boundary;

import static io.restassured.RestAssured.given;
import static org.junit.jupiter.api.Assertions.assertEquals;

import de.halbmann.sam.api.entity.collections.CollectionItemType;
import de.halbmann.sam.api.entity.collections.CreateCollectionItem;
import de.halbmann.sam.api.entity.collections.SheetCollection;
import de.halbmann.sam.api.entity.sheets.CreateSheetMusic;
import de.halbmann.sam.business.collections.controller.SheetCollectionService;
import de.halbmann.sam.business.sheets.controller.SheetService;
import io.quarkus.narayana.jta.QuarkusTransaction;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.security.TestSecurity;
import io.restassured.http.ContentType;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * An item can leave a collection from any position. The items behind it shift up, which Envers
 * audits as a DEL plus an ADD per shifted item in one revision: these only fit the audit table
 * because the position is part of its key.
 */
@QuarkusTest
@TestSecurity(
        user = "librarian1",
        roles = {"music_librarian"})
class CollectionItemRemovalTest {

    private static final String ITEMS = "/api/sheet-collections/{id}/items";

    @Inject
    SheetCollectionService collectionService;

    @Inject
    SheetService sheetService;

    @Inject
    EntityManager entityManager;

    private final List<String> sheetIds = new ArrayList<>();
    private String collectionId;

    @BeforeEach
    void createCollectionWithThreeSheets() {
        SheetCollection collection = new SheetCollection();
        collection.setName("Removal " + UUID.randomUUID());
        collectionId = collectionService.add(collection).getId().toString();
        for (int i = 0; i < 3; i++) {
            CreateSheetMusic sheet = ObjectMother.createSimpleSheetMusic();
            sheet.setTitle("Removal " + UUID.randomUUID());
            String sheetId = sheetService.addSheet(sheet).getId().toString();
            sheetIds.add(sheetId);
            CreateCollectionItem item = new CreateCollectionItem();
            item.setType(CollectionItemType.SHEET);
            item.setSheetId(UUID.fromString(sheetId));
            collectionService.addItem(collectionId, item);
        }
    }

    @AfterEach
    void deleteData() {
        collectionService.delete(collectionId);
        sheetIds.forEach(sheetService::deleteSheet);
    }

    private List<String> itemIds() {
        return given().get(ITEMS, collectionId).then().statusCode(200).extract().path("data.id");
    }

    private void removeItem(String itemId) {
        given().delete(ITEMS + "/{item}", collectionId, itemId).then().statusCode(204);
    }

    /** Items the audit history considers present at the end, by position. */
    private List<String> itemIdsInAuditHistory() {
        List<?> rows = QuarkusTransaction.requiringNew()
                .call(() -> entityManager
                        .createNativeQuery("SELECT CAST(items_id AS varchar) FROM sheet_collections_items_aud"
                                + " WHERE sheet_collections_id = :id AND revend IS NULL AND revtype <> 2"
                                + " ORDER BY items_order")
                        .setParameter("id", UUID.fromString(collectionId))
                        .getResultList());
        return rows.stream().map(String.class::cast).toList();
    }

    @ParameterizedTest
    @ValueSource(ints = {0, 1, 2})
    void removeItem_fromAnyPosition_keepsTheOthersInOrder(int position) {
        List<String> expected = new ArrayList<>(itemIds());
        String removed = expected.remove(position);

        removeItem(removed);

        assertEquals(expected, itemIds());
        assertEquals(expected, itemIdsInAuditHistory());
    }

    @Test
    void removeItems_oneAfterTheOther_fromTheFront() {
        List<String> ids = itemIds();

        removeItem(ids.get(0));
        removeItem(ids.get(1));

        assertEquals(List.of(ids.get(2)), itemIds());
        assertEquals(List.of(ids.get(2)), itemIdsInAuditHistory());
    }

    @Test
    void removeItem_afterAReorder() {
        List<String> ids = itemIds();
        List<String> reordered = List.of(ids.get(2), ids.get(0), ids.get(1));
        given().contentType(ContentType.JSON)
                .body(reordered)
                .put(ITEMS + "/order", collectionId)
                .then()
                .statusCode(204);

        removeItem(reordered.get(0));

        assertEquals(reordered.subList(1, 3), itemIds());
    }

    @ParameterizedTest
    @ValueSource(ints = {0, 1, 2})
    void deleteSheet_inAnyPosition_removesItsItem(int position) {
        List<String> expected = new ArrayList<>(itemIds());
        expected.remove(position);
        String sheetId = sheetIds.remove(position);

        given().delete("/api/sheets/{id}", sheetId).then().statusCode(204);

        assertEquals(expected, itemIds());
    }
}
