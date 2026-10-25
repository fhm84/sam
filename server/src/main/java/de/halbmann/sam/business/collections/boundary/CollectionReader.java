package de.halbmann.sam.business.collections.boundary;

import de.halbmann.sam.api.entity.collections.CollectionVisibility;
import de.halbmann.sam.business.collections.entity.SheetCollectionEntity;
import java.util.Map;

/**
 * Who reads collections, and so which of them they may see:
 *
 * <ul>
 *   <li>{@code WHOLE_ENSEMBLE} or unset: every login
 *   <li>{@code ADMINS_ONLY}: music librarian and admin
 *   <li>{@code PRIVATE}: the creator and the admin; without a recorded creator (collections from
 *       before it was recorded) also every music librarian
 * </ul>
 *
 * {@link #maySee} and {@link #condition} state the same rule, once in Java and once as JPQL.
 *
 * @param userId OIDC subject of the caller, {@code null} if unknown
 */
public record CollectionReader(boolean admin, boolean librarian, String userId) {

    public boolean maySee(final SheetCollectionEntity collection) {
        CollectionVisibility visibility = collection.getVisibility();
        if (visibility == null || visibility == CollectionVisibility.WHOLE_ENSEMBLE || admin) {
            return true;
        }
        if (!librarian) {
            return false;
        }
        return visibility == CollectionVisibility.ADMINS_ONLY
                || collection.getCreatedBy() == null
                || collection.getCreatedBy().equals(userId);
    }

    /**
     * JPQL condition limiting a query to the collections this reader may see, or {@code null} when
     * they see all. {@code path} prefixes the fields ({@code "sc."}, or {@code ""} in Panache
     * queries); the parameters it uses are added to {@code params}.
     */
    String condition(final String path, final Map<String, Object> params) {
        if (admin) {
            return null;
        }
        String visibility = path + "visibility";
        if (!librarian) {
            params.put("openToAll", CollectionVisibility.WHOLE_ENSEMBLE);
            return "(" + visibility + " is null or " + visibility + " = :openToAll)";
        }
        params.put("privateOnes", CollectionVisibility.PRIVATE);
        String createdBy = path + "createdBy";
        if (userId == null) {
            return "(" + visibility + " is null or " + visibility + " <> :privateOnes or " + createdBy + " is null)";
        }
        params.put("reader", userId);
        return "(" + visibility + " is null or " + visibility + " <> :privateOnes or " + createdBy + " is null or "
                + createdBy + " = :reader)";
    }
}
