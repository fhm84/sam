package de.halbmann.sam.api.boundary;

import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.jboss.resteasy.reactive.multipart.FileUpload;

@Path("documents")
public interface DocumentsResource {

    @GET
    @Path("{docIdentifier}")
    Response load(@PathParam("docIdentifier") String docIdentifier);

    @POST
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    String uploadDocument(@FormParam("file") FileUpload file);

}
