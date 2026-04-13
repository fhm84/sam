package de.halbmann.sam.core.exception;

/**
 * Business exception for missing entities (maps to 404)
 */
public class EntityNotFoundException extends RuntimeException {

    public EntityNotFoundException(String entityType, Object id) {
        super(entityType + " with id '" + id + "' not found");
    }
}
