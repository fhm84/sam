package de.halbmann.sam.api.boundary;

import de.halbmann.sam.api.entity.*;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

@Path("documents")
public interface DocumentsResource {

    @GET
    PaginatedResponse<Attachment> list(@BeanParam DocumentFilterRequest filterRequest);

    @GET
    @Path("unlinked")
    PaginatedResponse<DocumentDownload> listUnlinkedDocuments(@BeanParam DocumentFilterRequest filterRequest);

    @GET
    @Path("{docIdentifier}")
    @Produces(MediaType.APPLICATION_OCTET_STREAM)
    Response load(@PathParam("docIdentifier") String docIdentifier, @HeaderParam("If-None-Match") String ifNoneMatch);

    @POST
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    DocumentUpload uploadDocument(@BeanParam FileUploadRequest request);

    @DELETE
    @Path("{docIdentifier}")
    void delete(@PathParam("docIdentifier") String docIdentifier);
}
