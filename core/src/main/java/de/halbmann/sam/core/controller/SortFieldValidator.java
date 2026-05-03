package de.halbmann.sam.core.controller;

import de.halbmann.sam.core.exception.ValidationException;
import java.util.Set;

public final class SortFieldValidator {

    private SortFieldValidator() {}

    public static void validate(String[] sortBy, Set<String> allowed) {
        for (String field : sortBy) {
            if (!allowed.contains(field)) {
                throw new ValidationException("Invalid sort field: " + field);
            }
        }
    }
}
