package de.halbmann.sam.api.boundary;

import de.halbmann.sam.api.entity.*;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

/**
 * Resource endpoints for managing documents. This is used in multiple environments: standalone (for general/global documents upload/list), in context of a sheet and in context of an instrumentation.
 */
@Path("documents")
public interface DocumentsResource {

    @GET
    PaginatedResponse<Attachment> list(@BeanParam DocumentFilterRequest filterRequest);

    @GET
    @Path("unlinked")
    PaginatedResponse<DocumentDownload> listUnlinkedDocuments(@BeanParam DocumentFilterRequest filterRequest);

    /**
     * Download all documents in the current context (sheet / instrumentation) as a single file.
     * When called on a sheet context, pass {@code includeInstrumentations=true} to also include
     * all instrumentation-level attachments. The optional {@code type} parameter restricts to a
     * specific attachment type (e.g. PART). Supported formats: {@code zip} (default),
     * {@code merged-pdf} (PDFs only; non-PDFs are silently skipped).
     */
    @GET
    @Path("batch")
    @Produces({"application/zip", "application/pdf"})
    Response downloadBatch(
            @QueryParam("type") AttachmentType type,
            @QueryParam("includeInstrumentations") @DefaultValue("false") boolean includeInstrumentations,
            @QueryParam("format") @DefaultValue("ZIP") DownloadFormat format);

    /**
     * Download an arbitrary selection of attachments (by ID) as a single file.
     * Format: {@code zip} (default) or {@code merged-pdf}.
     * This endpoint is context-independent; the provided IDs are resolved directly.
     */
    @POST
    @Path("batch")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces({"application/zip", "application/pdf"})
    Response downloadBatchByIds(BatchDownloadRequest request);

    @GET
    @Path("{docIdentifier}")
    @Produces(MediaType.APPLICATION_OCTET_STREAM)
    Response load(@PathParam("docIdentifier") String docIdentifier, @HeaderParam("If-None-Match") String ifNoneMatch);

    @POST
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    DocumentUpload uploadDocument(@BeanParam FileUploadRequest request);

    @POST
    @Path("{docIdentifier}/link")
    void linkDocument(@PathParam("docIdentifier") String docIdentifier, DocumentLinkRequest documentLink);

    /**
     * Delete the document identified by given identifier. If sheetId and/or instrumentationId is set, given id is interpreted as attachment id (only removing the "business" link - the attachment), else it's interpreted as documentId. If and only if the document is not linked anywhere (refCount = 0), the document is physically deleted.
     *
     * @param docIdentifier
     */
    @DELETE
    @Path("{docIdentifier}")
    void delete(@PathParam("docIdentifier") String docIdentifier);
}
