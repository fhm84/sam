package de.halbmann.sam.api.boundary;

import de.halbmann.sam.api.entity.DocumentDownload;
import de.halbmann.sam.api.entity.DocumentFilterRequest;
import de.halbmann.sam.api.entity.FileUploadRequest;
import de.halbmann.sam.api.entity.PaginatedResponse;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

@Path("documents")
@RegisterRestClient(configKey = "documents-api")
public interface DocumentsResource {

  @GET
  PaginatedResponse<DocumentDownload> list(@BeanParam DocumentFilterRequest filterRequest);

  @GET
  @Path("{docIdentifier}")
  @Produces(MediaType.APPLICATION_OCTET_STREAM)
  Response load(
      @PathParam("docIdentifier") String docIdentifier,
      @HeaderParam("If-None-Match") String ifNoneMatch);

  @POST
  @Consumes(MediaType.MULTIPART_FORM_DATA)
  String uploadDocument(@BeanParam FileUploadRequest request);
}
