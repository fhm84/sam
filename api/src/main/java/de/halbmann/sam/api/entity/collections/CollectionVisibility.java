package de.halbmann.sam.api.entity.collections;

/** Controls which ensemble members can see a collection. */
public enum CollectionVisibility {
    /** Visible to all members of the ensemble. */
    WHOLE_ENSEMBLE,
    /** Visible only to users with the admin or music-librarian role. */
    ADMINS_ONLY,
    /** Visible only to the user who created the collection. */
    PRIVATE
}
