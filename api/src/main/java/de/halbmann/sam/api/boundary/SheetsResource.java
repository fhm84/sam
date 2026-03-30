package de.halbmann.sam.api.boundary;

import de.halbmann.sam.api.entity.*;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.util.List;
import java.util.Set;

/**
 * RESTful API for managing sheet music resources. Provides operations to find, add, load, update,
 * and delete sheet music.
 */
@Path("sheets")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public interface SheetsResource {

    /**
     * Finds sheets based on the provided filter request.
     *
     * @param filterRequest the filter criteria for searching sheets
     * @return a paginated response containing the list of sheet music
     */
    @GET
    PaginatedResponse<SheetMusicSearchResult> findSheets(final @BeanParam SheetFilterRequest filterRequest);

    /**
     * Adds a new sheet music entry.
     *
     * @param sheetMusic the sheet music to add
     * @return the added sheet music
     */
    @POST
    SheetMusic add(final CreateSheetMusic sheetMusic);

    /**
     * Loads a specific sheet music by its ID.
     *
     * @param sheetId the ID of the sheet music to load
     * @return the sheet music with the specified ID
     */
    @GET
    @Path("{sheetId}")
    SheetMusic load(final @PathParam("sheetId") String sheetId);

    /**
     * Updates an existing sheet music entry.
     *
     * @param sheetId    the ID of the sheet music to update
     * @param sheetMusic the updated sheet music data
     */
    @PUT
    @Path("{sheetId}")
    void update(final @PathParam("sheetId") String sheetId, final SheetMusic sheetMusic);

    /**
     * Deletes a specific sheet music by its ID.
     *
     * @param sheetId the ID of the sheet music to delete
     */
    @DELETE
    @Path("{sheetId}")
    void delete(final @PathParam("sheetId") String sheetId);

    /**
     * Add one or more tags to a sheet music entity.
     */
    @POST
    @Path("{sheetId}/tags")
    void addTags(final @PathParam("sheetId") String sheetId, Set<String> newTags);

    /**
     * Remove one or more tags from a sheet music entity.
     */
    @DELETE
    @Path("{sheetId}/tags")
    void removeTag(final @PathParam("sheetId") String sheetId, Set<String> tagsToRemove);

    /**
     * Favorite the sheet music.
     *
     * @param sheetId the ID of the sheet music
     */
    @POST
    @Path("{sheetId}/favorite")
    void favorite(final @PathParam("sheetId") String sheetId);

    /**
     * Unfavorite the sheet music.
     *
     * @param sheetId the ID of the sheet music
     */
    @DELETE
    @Path("{sheetId}/favorite")
    void unfavorite(final @PathParam("sheetId") String sheetId);

    /**
     * Returns all distinct genre values used across sheet music.
     *
     * @return a sorted list of genre strings
     */
    @GET
    @Path("genres")
    List<String> getGenres();

    /**
     * Returns all distinct first letters of sheet music titles, optionally filtered by genre.
     *
     * @param genre optional genre to restrict the letter set
     * @return a sorted list of single uppercase letters
     */
    @GET
    @Path("letters")
    List<String> getAvailableLetters(@QueryParam("genre") String genre);

    /**
     * Returns all collections that contain the given sheet music.
     *
     * @param sheetId the ID of the sheet music
     * @param paginationRequest pagination parameters
     * @return paginated list of collections containing this sheet
     */
    @GET
    @Path("{sheetId}/collections")
    PaginatedResponse<SheetCollection> getCollections(
            final @PathParam("sheetId") String sheetId, final @BeanParam PaginationRequest paginationRequest);

    /**
     * Provides access to the instrumentations resource for a specific sheet music.
     *
     * @param sheetId the ID of the sheet music
     * @return the instrumentations resource
     */
    @Path("{sheetId}/instrumentations")
    InstrumentationsResource instrumentations(final @PathParam("sheetId") String sheetId);

    /**
     * Provides access to the documents resource for a speciic sheet music.
     *
     * @param sheetId the ID of the sheet music
     * @return the documents resource
     */
    @Path("{sheetId}/documents")
    DocumentsResource documents(final @PathParam("sheetId") String sheetId);

    /**
     * Evaluates the coverage of a sheet music's instrumentations against an ensemble definition.
     *
     * @param sheetId    the ID of the sheet music
     * @param ensembleId the ID of the ensemble to evaluate against
     * @return the coverage result with per-voice breakdown
     */
    @GET
    @Path("{sheetId}/coverage")
    CoverageResult coverage(
            final @PathParam("sheetId") String sheetId, final @QueryParam("ensemble") String ensembleId);

    /**
     * Runs AI-powered data enrichment on an existing sheet music entry and returns suggestions
     * for missing or complementary metadata (tags, style, difficulty, notes, year).
     *
     * @param sheetId the ID of the sheet music to enrich
     * @return enrichment suggestions for user review
     */
    @POST
    @Path("{sheetId}/enrich")
    SheetEnrichment enrich(final @PathParam("sheetId") String sheetId);

    /**
     * Exports a sheet music entry as a downloadable file.
     *
     * <ul>
     *   <li>{@code ZIP} (default) — metadata JSON + all attachment files in a ZIP archive</li>
     *   <li>{@code JSON} — sheet metadata as a JSON download</li>
     *   <li>{@code CSV} — sheet metadata as a single-row CSV download</li>
     * </ul>
     *
     * @param sheetId the ID of the sheet music to export
     * @param format  the export format (ZIP, JSON, CSV); defaults to ZIP
     * @return the exported file as a download response
     */
    @GET
    @Path("{sheetId}/export")
    @Produces(MediaType.WILDCARD)
    Response export(
            final @PathParam("sheetId") String sheetId,
            final @QueryParam("format") @DefaultValue("ZIP") ExportFormat format);
}
