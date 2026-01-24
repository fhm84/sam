package de.halbmann.sam.api.boundary;

import de.halbmann.sam.api.entity.PaginatedResponse;
import de.halbmann.sam.api.entity.SheetFilterRequest;
import de.halbmann.sam.api.entity.SheetMusic;
import de.halbmann.sam.api.entity.SheetMusicSearchResult;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

/**
 * RESTful API for managing sheet music resources. Provides operations to find, add, load, update,
 * and delete sheet music.
 */
@Path("sheets")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@RegisterRestClient(configKey = "sheets-api")
public interface SheetsResource {

  /**
   * Finds sheets based on the provided filter request.
   *
   * @param filterRequest the filter criteria for searching sheets
   * @return a paginated response containing the list of sheet music
   */
  @GET
  PaginatedResponse<SheetMusicSearchResult> findSheets(
      final @BeanParam SheetFilterRequest filterRequest);

  /**
   * Adds a new sheet music entry.
   *
   * @param sheetMusic the sheet music to add
   * @return the added sheet music
   */
  @POST
  SheetMusic add(final SheetMusic sheetMusic);

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
   * @param sheetId the ID of the sheet music to update
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
}
