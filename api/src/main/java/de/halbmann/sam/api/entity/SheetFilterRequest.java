package de.halbmann.sam.api.entity;

import jakarta.ws.rs.QueryParam;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SheetFilterRequest extends PaginationRequest {

    /**
     * For "generic" search/query
     */
    @QueryParam("q")
    private String query;

    /**
     * Search by title
     */
    @QueryParam("title")
    private String title;

    /**
     * Search by composer
     */
    @QueryParam("composer")
    private String composer;

    /**
     * Filter by genre
     */
    @QueryParam("genre")
    private String genre;

    /**
     * Filter by first letter of title (case-insensitive prefix)
     */
    @QueryParam("titleStartsWith")
    private String titleStartsWith;
}
