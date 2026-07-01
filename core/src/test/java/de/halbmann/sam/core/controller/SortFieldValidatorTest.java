package de.halbmann.sam.core.controller;

import static org.junit.jupiter.api.Assertions.*;

import de.halbmann.sam.core.exception.ValidationException;
import java.util.Set;
import org.junit.jupiter.api.Test;

class SortFieldValidatorTest {

    private static final Set<String> ALLOWED = Set.of("title", "composer", "genre");

    @Test
    void validFieldPasses() {
        assertDoesNotThrow(() -> SortFieldValidator.validate(new String[] {"title"}, ALLOWED));
    }

    @Test
    void multipleValidFieldsPass() {
        assertDoesNotThrow(() -> SortFieldValidator.validate(new String[] {"title", "genre"}, ALLOWED));
    }

    @Test
    void emptyArrayPasses() {
        assertDoesNotThrow(() -> SortFieldValidator.validate(new String[] {}, ALLOWED));
    }

    @Test
    void invalidFieldThrows() {
        assertThrows(ValidationException.class, () -> SortFieldValidator.validate(new String[] {"unknown"}, ALLOWED));
    }

    @Test
    void mixedValidAndInvalidThrows() {
        assertThrows(
                ValidationException.class,
                () -> SortFieldValidator.validate(new String[] {"title", "invalid"}, ALLOWED));
    }

    @Test
    void errorMessageContainsFieldName() {
        ValidationException ex = assertThrows(
                ValidationException.class, () -> SortFieldValidator.validate(new String[] {"badField"}, ALLOWED));
        assertTrue(ex.getMessage().contains("badField"));
    }
}
