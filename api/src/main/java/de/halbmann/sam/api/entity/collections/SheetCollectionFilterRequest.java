package de.halbmann.sam.api.entity.collections;

import de.halbmann.sam.api.entity.shared.PaginationRequest;
import jakarta.ws.rs.QueryParam;
import lombok.Getter;
import lombok.Setter;

/**
 * Filter and pagination parameters for the sheet collection list endpoint. Extends
 * {@link PaginationRequest} with a full-text search field ({@code q}), a name filter, and a
 * collection-type filter.
 */
@Getter
@Setter
public class SheetCollectionFilterRequest extends PaginationRequest {

    /**
     * For "generic" search/query
     */
    @QueryParam("q")
    private String query;

    /**
     * Search by name
     */
    @QueryParam("name")
    private String name;

    /**
     * Filter by collection type
     */
    @QueryParam("type")
    private CollectionType type;
}
