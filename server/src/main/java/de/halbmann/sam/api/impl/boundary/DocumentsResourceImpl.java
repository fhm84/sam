package de.halbmann.sam.api.impl.boundary;

import de.halbmann.sam.api.boundary.DocumentsResource;
import de.halbmann.sam.api.entity.*;
import de.halbmann.sam.business.controller.DocumentsService;
import de.halbmann.sam.business.entity.AttachmentEntity;
import de.halbmann.sam.business.entity.DocumentEntity;
import de.halbmann.sam.classification.controller.DocumentClassificationService;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.InternalServerErrorException;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.StreamingOutput;
import java.io.IOException;
import java.io.InputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.stream.Stream;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.event.Level;

@Slf4j
@RequestScoped
public class DocumentsResourceImpl implements DocumentsResource {

    @Inject
    DocumentsService documentsService;

    @Inject
    DocumentClassificationService classificationService;

    @PathParam("instrumentationId")
    String instrumentationId;

    @PathParam("sheetId")
    String sheetId;

    @Override
    public PaginatedResponse<Attachment> list(DocumentFilterRequest filterRequest) {
        List<Attachment> attachments;
        if (instrumentationId != null) {
            attachments = documentsService.loadAttachmentsByInstrumentation(instrumentationId);
        } else if (sheetId != null) {
            attachments = documentsService.loadAttachmentsBySheet(sheetId);
        } else {
            attachments = new ArrayList<>();
        }
        // TODO: maybe we could/should also implement the filter to not only show unlinked documents?
        PaginatedResponse<Attachment> response = new PaginatedResponse<>();
        response.setData(attachments);
        response.setPage(filterRequest.getPage());
        response.setSize(response.getData().size());
        response.setTotalCount(response.getData().size());
        return response;
    }

    @Override
    public PaginatedResponse<DocumentDownload> listUnlinkedDocuments(DocumentFilterRequest filterRequest) {
        List<DocumentEntity> unlinkedDocuments = documentsService.listUnlinkedDocuments();
        PaginatedResponse<DocumentDownload> response = new PaginatedResponse<>();
        response.setData(unlinkedDocuments.stream()
                .map(d -> new DocumentDownload(
                        null, d.getId(), d.getFilename(), d.getSize(), d.getMimeType(), d.getSha256()))
                .toList());
        response.setPage(filterRequest.getPage());
        response.setSize(response.getData().size());
        response.setTotalCount(response.getData().size());
        return response;
    }

    @Override
    public Response downloadBatch(AttachmentType type, boolean includeInstrumentations, DownloadFormat format) {
        List<AttachmentEntity> attachments;
        String zipName;

        if (instrumentationId != null) {
            attachments = documentsService.loadAttachmentEntitiesByInstrumentation(instrumentationId, type);
            zipName = "instrumentation-" + instrumentationId;
        } else if (sheetId != null) {
            List<AttachmentEntity> sheetAtts = documentsService.loadAttachmentEntitiesBySheet(sheetId, type);
            if (includeInstrumentations) {
                List<AttachmentEntity> instrAtts =
                        documentsService.loadAttachmentEntitiesBySheetInstrumentations(sheetId, type);
                attachments =
                        Stream.concat(sheetAtts.stream(), instrAtts.stream()).toList();
            } else {
                attachments = sheetAtts;
            }
            zipName = "sheet-" + sheetId + (type != null ? "-" + type.name().toLowerCase() : "");
        } else {
            return Response.status(Response.Status.BAD_REQUEST).build();
        }

        if (attachments.isEmpty()) {
            return Response.noContent().build();
        }

        return buildResponse(attachments, format, zipName);
    }

    @Override
    public Response downloadBatchByIds(BatchDownloadRequest request) {
        if (request == null || request.getIds() == null || request.getIds().isEmpty()) {
            return Response.status(Response.Status.BAD_REQUEST).build();
        }

        List<AttachmentEntity> attachments = documentsService.loadAttachmentEntitiesByIds(request.getIds());
        if (attachments.isEmpty()) {
            return Response.noContent().build();
        }

        String baseName =
                request.getBaseName() != null && !request.getBaseName().isBlank() ? request.getBaseName() : "documents";
        return buildResponse(attachments, request.getFormat(), baseName);
    }

    private Response buildResponse(List<AttachmentEntity> attachments, DownloadFormat format, String baseName) {
        if (format == DownloadFormat.MERGED_PDF) {
            StreamingOutput pdf = documentsService.buildMergedPdf(attachments);
            if (pdf != null) {
                String filename = URLEncoder.encode(baseName + ".pdf", StandardCharsets.UTF_8)
                        .replace("+", "%20");
                return Response.ok(pdf)
                        .type("application/pdf")
                        .header("Content-Disposition", "attachment; filename*=utf-8''" + filename)
                        .header("Cache-Control", "no-store")
                        .build();
            }
            // Fall through to ZIP if no PDFs found
            log.info("No PDF attachments found for merged-pdf request — falling back to ZIP");
        }

        StreamingOutput zip = documentsService.buildZip(attachments, baseName);
        String filename =
                URLEncoder.encode(baseName + ".zip", StandardCharsets.UTF_8).replace("+", "%20");
        return Response.ok(zip)
                .type("application/zip")
                .header("Content-Disposition", "attachment; filename*=utf-8''" + filename)
                .header("Cache-Control", "no-store")
                .build();
    }

    @Override
    public Response load(String docIdentifier, String ifNoneMatch) {
        DocumentDownload attachment;
        if (instrumentationId != null) {
            attachment = documentsService.loadAttachmentByInstrumentation(instrumentationId, docIdentifier);
        } else if (sheetId != null) {
            attachment = documentsService.loadAttachmentBySheet(sheetId, docIdentifier);
        } else {
            attachment = Optional.ofNullable(documentsService.loadAttachment(docIdentifier))
                    .orElse(documentsService.load(UUID.fromString(docIdentifier)));
        }
        if (attachment == null) {
            log.info("Document ({}) not found", docIdentifier);
            return Response.status(Response.Status.NOT_FOUND).build();
        }

        String etagValue = "\"sha256:" + attachment.checksumSha256() + "\"";

        // If-None-Match handling
        if (etagMatches(ifNoneMatch, etagValue)) {
            return Response.notModified().tag(etagValue).build();
        }

        try {
            final StreamingOutput streamingOutput = output -> {
                try (InputStream in = attachment.stream()) {
                    in.transferTo(output);
                }
            };
            final String encodedFilename = URLEncoder.encode(attachment.filename(), StandardCharsets.UTF_8);
            return Response.ok(streamingOutput)
                    .type(attachment.mimeType())
                    .header("Content-Length", attachment.size())
                    .header("X-Checksum-SHA256", attachment.checksumSha256())
                    .header("Cache-Control", "private, max-age=3600")
                    .header(
                            "Content-Disposition",
                            "inline; filename*=utf-8''" + encodedFilename.replaceAll("\\+", "%20"))
                    .tag(etagValue)
                    .build();
        } catch (final Exception ex) {
            log.atInfo().setCause(ex).log(() -> String.format("Loading document (%s) resulted in", docIdentifier));
            return Response.noContent().build();
        }
    }

    @Override
    public DocumentUpload uploadDocument(FileUploadRequest request) {
        log.atLevel(Level.DEBUG)
                .log(() -> "File-Upload ... filename: " + request.getFile().fileName());

        try (InputStream inputStream = Files.newInputStream(request.getFile().uploadedFile())) {
            DocumentUpload upload = documentsService.save(request.getFile().fileName(), inputStream, request.getType());

            if (sheetId != null || instrumentationId != null) {
                DocumentLinkRequest documentLinkRequest = new DocumentLinkRequest();
                if (sheetId != null) documentLinkRequest.setSheetId(UUID.fromString(sheetId));
                if (instrumentationId != null)
                    documentLinkRequest.setInstrumentationId(UUID.fromString(instrumentationId));
                if (request.getType() != null) documentLinkRequest.setAttachmentType(request.getType());
                Attachment attachment =
                        documentsService.linkDocument(upload.document().id(), documentLinkRequest);

                log.atLevel(Level.INFO)
                        .log(() -> "File uploaded and linked - filename: "
                                + request.getFile().fileName() + " (" + attachment.getId() + ")");
                return new DocumentUpload(upload.document(), attachment);
            } else {
                log.atLevel(Level.INFO)
                        .log(() ->
                                "File uploaded - filename: " + request.getFile().fileName());
                return upload;
            }
        } catch (IOException | NoSuchAlgorithmException e) {
            log.atWarn()
                    .setCause(e)
                    .log(() -> "Failed to upload file " + request.getFile().fileName());
            throw new InternalServerErrorException("Failed to save file", e);
        }
    }

    @Override
    public Attachment linkDocument(String docIdentifier, DocumentLinkRequest documentLink) {
        return documentsService.linkDocument(UUID.fromString(docIdentifier), documentLink);
    }

    @Override
    public SheetClassification classify(String docIdentifier) {
        return classificationService.classify(UUID.fromString(docIdentifier));
    }

    @Override
    public ClassificationApplyResult applyClassification(String docIdentifier, ClassificationApplyRequest request) {
        return classificationService.apply(UUID.fromString(docIdentifier), request);
    }

    @Override
    public void delete(String docIdentifier) {
        if (sheetId == null && instrumentationId == null) {
            documentsService.deleteIfUnlinked(UUID.fromString(docIdentifier));
        } else {
            documentsService.deleteAttachment(docIdentifier);
        }
    }

    private boolean etagMatches(String ifNoneMatch, String currentEtag) {
        if (ifNoneMatch == null || ifNoneMatch.isBlank()) {
            return false;
        }

        if ("*".equals(ifNoneMatch.trim())) {
            return true;
        }

        return Arrays.stream(ifNoneMatch.split(","))
                .map(String::trim)
                .anyMatch(tag -> tag.equals(currentEtag) || tag.equals("W/" + currentEtag));
    }
}
