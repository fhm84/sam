package de.halbmann.sam.api.boundary;

import de.halbmann.sam.api.entity.shared.PaginatedResponse;
import de.halbmann.sam.api.entity.sheets.Booklet;
import de.halbmann.sam.api.entity.sheets.BookletFilterRequest;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;

/**
 * REST API for managing booklets. A booklet is a physical binder or printed compilation that
 * groups sheet music entries by position. Sheet entries within a booklet are managed via the
 * {@code sheets} sub-resource ({@link CollectionSheetsResource}).
 */
@Path("booklets")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public interface BookletsResource {

    /**
     * Returns a paginated list of booklets matching the given filter criteria.
     *
     * @param filterRequest pagination and filter parameters
     * @return paginated list of matching booklets
     */
    @GET
    PaginatedResponse<Booklet> findBooklets(@BeanParam BookletFilterRequest filterRequest);

    /**
     * Creates a new booklet.
     *
     * @param booklet the booklet to create
     * @return the persisted booklet including its generated ID
     */
    @POST
    Booklet add(Booklet booklet);

    /**
     * Loads a single booklet by its ID.
     *
     * @param bookletId the booklet ID
     * @return the booklet
     */
    @GET
    @Path("{bookletId}")
    Booklet load(@PathParam("bookletId") String bookletId);

    /**
     * Updates an existing booklet.
     *
     * @param bookletId the booklet ID
     * @param booklet   the updated booklet data
     */
    @PUT
    @Path("{bookletId}")
    void update(@PathParam("bookletId") String bookletId, Booklet booklet);

    /**
     * Deletes a booklet by its ID.
     *
     * @param bookletId the booklet ID
     */
    @DELETE
    @Path("{bookletId}")
    void delete(@PathParam("bookletId") String bookletId);

    /**
     * Provides access to the sheet entries within the given booklet.
     *
     * @param bookletId the booklet ID
     * @return the sheets sub-resource
     */
    @Path("{bookletId}/sheets")
    CollectionSheetsResource sheets(@PathParam("bookletId") String bookletId);
}
