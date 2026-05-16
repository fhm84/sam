package de.halbmann.sam.api.boundary;

import de.halbmann.sam.api.entity.shared.PaginatedResponse;
import de.halbmann.sam.api.entity.shared.PaginationRequest;
import de.halbmann.sam.api.entity.sheets.SheetWithMyParts;
import jakarta.ws.rs.BeanParam;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

/**
 * Returns the sheets that contain at least one instrumentation matching the calling musician's
 * instruments across their ensemble memberships.
 */
@Path("me/parts")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public interface MyPartsResource {

    @GET
    PaginatedResponse<SheetWithMyParts> getMyParts(@BeanParam PaginationRequest filter);
}
