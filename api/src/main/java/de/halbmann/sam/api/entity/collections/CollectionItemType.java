package de.halbmann.sam.api.entity.collections;

/**
 * Discriminates the two kinds of items that can appear in a sheet collection: a reference to an
 * existing sheet music entry ({@code SHEET}) or a standalone free-text block ({@code TEXT}).
 */
public enum CollectionItemType {
    SHEET,
    TEXT
}
