package de.halbmann.sam.api.boundary;

import de.halbmann.sam.api.entity.documents.AttachmentType;
import de.halbmann.sam.api.entity.shares.PublicShareInfo;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.util.UUID;

/**
 * Unauthenticated endpoints for share-link access. Token validation is performed per request; no
 * OIDC token is required.
 */
@Path("public/shares")
@Produces(MediaType.APPLICATION_JSON)
public interface PublicShareResource {

    @GET
    @Path("{token}")
    PublicShareInfo getInfo(@PathParam("token") UUID token);

    /**
     * Download endpoint for INSTRUMENTATION shares — streams the part file(s). For SHEET shares,
     * streams a flat ZIP of all instrumentation parts, optionally filtered by attachment type
     * (defaults to {@link AttachmentType#PART}). For COLLECTION shares, streams a ZIP of all
     * instrumentation parts across all sheets.
     */
    @GET
    @Path("{token}/download")
    @Produces(MediaType.WILDCARD)
    Response download(@PathParam("token") UUID token, @QueryParam("type") AttachmentType type);

    /** TOC PDF download for COLLECTION shares. */
    @GET
    @Path("{token}/toc")
    @Produces(MediaType.WILDCARD)
    Response downloadToc(@PathParam("token") UUID token);

    /** Download a specific instrumentation part within a COLLECTION or SHEET share. */
    @GET
    @Path("{token}/download/{instrumentationId}")
    @Produces(MediaType.WILDCARD)
    Response downloadPart(@PathParam("token") UUID token, @PathParam("instrumentationId") UUID instrumentationId);

    /** Download a single sheet-level attachment within a SHEET share. */
    @GET
    @Path("{token}/sheet/{attachmentId}")
    @Produces(MediaType.WILDCARD)
    Response downloadSheetAttachment(@PathParam("token") UUID token, @PathParam("attachmentId") UUID attachmentId);
}
