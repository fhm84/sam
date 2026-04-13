package de.halbmann.sam.api.entity.sheets;

import de.halbmann.sam.api.entity.shared.PaginationRequest;
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

    /**
     * Filter for (non) favorite sheets.
     */
    @QueryParam("favorite")
    private Boolean favorite;

    /**
     * Ensemble context — when set, coverage snapshots are attached to each result.
     */
    @QueryParam("ensemble")
    private String ensemble;
}
