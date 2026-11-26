package de.halbmann.sam.api.entity;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasItems;

import de.halbmann.sam.api.entity.collections.CollectionItem;
import de.halbmann.sam.api.entity.collections.CollectionSheet;
import de.halbmann.sam.api.entity.collections.SheetCollection;
import de.halbmann.sam.api.entity.sheets.Booklet;
import de.halbmann.sam.api.entity.sheets.Instrumentation;
import de.halbmann.sam.api.entity.sheets.SheetMusic;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;

/**
 * The API entities' lists carry {@code @Valid} on the type argument ({@code List<@Valid X>}), not
 * on the container (deprecated, HV000271): validation must still cascade into each element.
 */
@QuarkusTest
class NestedListValidationTest {

    @Inject
    Validator validator;

    @Test
    void sheetMusic_validatesEachInstrumentation() {
        SheetMusic sheet = new SheetMusic();
        sheet.getInstrumentations().add(new Instrumentation()); // instrument is @NotNull

        assertThat(paths(validator.validate(sheet)), hasItem("instrumentations[0].instrument"));
    }

    @Test
    void sheetCollection_validatesEachItem() {
        SheetCollection collection = new SheetCollection();
        collection.getItems().add(new CollectionItem()); // type @NotNull, identifier @NotBlank

        assertThat(paths(validator.validate(collection)), hasItems("items[0].type", "items[0].identifier"));
    }

    @Test
    void booklet_validatesEachSheet() {
        Booklet booklet = new Booklet();
        booklet.getSheets().add(new CollectionSheet()); // identifier @NotBlank

        assertThat(paths(validator.validate(booklet)), hasItem("sheets[0].identifier"));
    }

    private static Set<String> paths(Set<? extends ConstraintViolation<?>> violations) {
        return violations.stream().map(v -> v.getPropertyPath().toString()).collect(Collectors.toSet());
    }
}
