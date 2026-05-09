package de.halbmann.sam.api.entity.sheets;

import de.halbmann.sam.api.entity.shared.PaginationRequest;
import jakarta.ws.rs.QueryParam;
import lombok.Getter;
import lombok.Setter;

/**
 * Filter and pagination parameters for the sheet music list endpoint. Extends
 * {@link PaginationRequest} with full-text search ({@code q}), field-specific filters (title,
 * composer, genre, first-letter prefix), a favorites flag, and an optional ensemble context for
 * attaching coverage snapshots to each result.
 */
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
