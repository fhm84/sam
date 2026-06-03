package de.halbmann.sam.api.boundary;

import de.halbmann.sam.api.entity.classification.ClassificationApplyRequest;
import de.halbmann.sam.api.entity.classification.ClassificationApplyResult;
import de.halbmann.sam.api.entity.classification.SheetClassification;
import de.halbmann.sam.api.entity.documents.*;
import de.halbmann.sam.api.entity.shared.PaginatedResponse;
import de.halbmann.sam.api.entity.sheets.BatchDownloadRequest;
import de.halbmann.sam.api.entity.sheets.DownloadFormat;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

/**
 * Resource endpoints for managing documents. Usable in three contexts: standalone (global unlinked
 * documents), within a sheet ({@code /sheets/{id}/documents}), or within an instrumentation
 * ({@code /sheets/{id}/instrumentations/{id}/documents}).
 */
@Path("documents")
public interface DocumentsResource {

    /**
     * Lists attachments in the current context (sheet, instrumentation, or global).
     *
     * @param filterRequest pagination parameters
     * @return paginated list of attachments
     */
    @GET
    PaginatedResponse<Attachment> list(@BeanParam DocumentFilterRequest filterRequest);

    /**
     * Lists documents that have been uploaded but are not yet linked to any sheet or instrumentation.
     *
     * @param filterRequest pagination parameters
     * @return paginated list of unlinked documents
     */
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

    /**
     * Downloads the raw file for an attachment or document. Supports conditional GET via
     * {@code If-None-Match} / ETag to allow efficient client-side caching.
     *
     * @param docIdentifier the attachment or document ID
     * @param ifNoneMatch   optional ETag from a previous response for cache validation
     * @return the file content, or 304 Not Modified if the ETag matches
     */
    @GET
    @Path("{docIdentifier}")
    @Produces(MediaType.APPLICATION_OCTET_STREAM)
    Response load(@PathParam("docIdentifier") String docIdentifier, @HeaderParam("If-None-Match") String ifNoneMatch);

    /**
     * Uploads a new document file. The document is stored and returned as an unlinked record until
     * explicitly linked to a sheet or instrumentation via {@code POST /{id}/link}.
     *
     * @param request the multipart form data containing the file and optional attachment type
     * @return the created document and its initial (unlinked) attachment record
     */
    @POST
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    DocumentUpload uploadDocument(@BeanParam FileUploadRequest request);

    /**
     * Link or relink a document/attachment to a sheet or instrumentation.
     * <ul>
     *   <li>If {@code docIdentifier} is an existing <em>attachment</em> ID, the attachment is moved
     *       to the new target and its type is updated if provided (relink / correct a wrong link).</li>
     *   <li>If {@code docIdentifier} is a <em>document</em> ID, a new attachment is created and
     *       linked to the specified target (original behaviour).</li>
     * </ul>
     * Passing neither {@code sheetId} nor {@code instrumentationId} in the body moves the attachment
     * to the unlinked pool without deleting the physical file.
     */
    @POST
    @Path("{docIdentifier}/link")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    Attachment linkDocument(@PathParam("docIdentifier") String docIdentifier, DocumentLinkRequest documentLink);

    /**
     * Run AI-based classification on the stored document.
     * Returns detected metadata and pre-matched entity references.
     * Only applicable to documents with MIME type {@code application/pdf} or image types.
     */
    @POST
    @Path("{docIdentifier}/classify")
    @Produces(MediaType.APPLICATION_JSON)
    SheetClassification classify(@PathParam("docIdentifier") String docIdentifier);

    /**
     * Apply a reviewed classification result: creates/resolves entities and links the document
     * as an attachment to the resulting sheet or instrumentation.
     */
    @POST
    @Path("{docIdentifier}/apply")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    ClassificationApplyResult applyClassification(
            @PathParam("docIdentifier") String docIdentifier, ClassificationApplyRequest request);

    /**
     * Deletes a document or attachment. When the identifier refers to an attachment, only that
     * business link is removed. The underlying physical file is deleted only when no other
     * attachments reference it (ref-count reaches zero).
     *
     * @param docIdentifier the attachment or document ID
     */
    @DELETE
    @Path("{docIdentifier}")
    void delete(@PathParam("docIdentifier") String docIdentifier);
}
