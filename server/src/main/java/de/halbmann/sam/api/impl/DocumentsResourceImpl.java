package de.halbmann.sam.api.impl;

import de.halbmann.sam.api.boundary.DocumentsResource;
import de.halbmann.sam.api.entity.classification.ClassificationApplyRequest;
import de.halbmann.sam.api.entity.classification.ClassificationApplyResult;
import de.halbmann.sam.api.entity.classification.SheetClassification;
import de.halbmann.sam.api.entity.documents.*;
import de.halbmann.sam.api.entity.eventlog.EventType;
import de.halbmann.sam.api.entity.shared.PaginatedResponse;
import de.halbmann.sam.api.entity.sheets.BatchDownloadRequest;
import de.halbmann.sam.api.entity.sheets.DownloadFormat;
import de.halbmann.sam.business.documents.controller.DocumentsService;
import de.halbmann.sam.business.documents.controller.MergedPdfEntry;
import de.halbmann.sam.business.documents.controller.StreamWriter;
import de.halbmann.sam.business.documents.entity.AttachmentEntity;
import de.halbmann.sam.business.documents.entity.DocumentEntity;
import de.halbmann.sam.business.eventlog.controller.EventLogService;
import de.halbmann.sam.classification.controller.DocumentClassificationService;
import de.halbmann.sam.security.Roles;
import io.quarkus.security.Authenticated;
import jakarta.annotation.security.RolesAllowed;
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
import lombok.extern.slf4j.Slf4j;
import org.slf4j.event.Level;

@Slf4j
@Authenticated
@RequestScoped
public class DocumentsResourceImpl implements DocumentsResource {

    @Inject
    DocumentsService documentsService;

    @Inject
    DocumentClassificationService classificationService;

    @Inject
    EventLogService eventLogService;

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
        List<MergedPdfEntry> entries;
        String zipName;

        if (instrumentationId != null) {
            entries = documentsService.buildMergeEntriesForInstrumentation(instrumentationId, type);
            zipName = "instrumentation-" + instrumentationId;
        } else if (sheetId != null) {
            entries = documentsService.buildMergeEntriesForSheet(sheetId, type, includeInstrumentations);
            zipName = "sheet-" + sheetId + (type != null ? "-" + type.name().toLowerCase() : "");
        } else {
            return Response.status(Response.Status.BAD_REQUEST).build();
        }

        if (entries.isEmpty()) {
            return Response.noContent().build();
        }

        eventLogService.log(
                EventType.DOCUMENT_BATCH_DOWNLOAD,
                instrumentationId != null ? "instrumentation" : "sheet",
                instrumentationId != null ? UUID.fromString(instrumentationId) : UUID.fromString(sheetId),
                Map.of("count", entries.size(), "format", format.name()));

        return buildResponse(entries, format, zipName);
    }

    @Override
    public Response downloadBatchByIds(BatchDownloadRequest request) {
        if (request == null || request.getIds() == null || request.getIds().isEmpty()) {
            return Response.status(Response.Status.BAD_REQUEST).build();
        }

        List<MergedPdfEntry> entries = documentsService.buildMergeEntriesById(request.getIds());
        if (entries.isEmpty()) {
            return Response.noContent().build();
        }

        eventLogService.log(
                EventType.DOCUMENT_BATCH_DOWNLOAD,
                "attachment",
                null,
                Map.of(
                        "count",
                        entries.size(),
                        "format",
                        request.getFormat() != null ? request.getFormat().name() : "ZIP"));

        String baseName =
                request.getBaseName() != null && !request.getBaseName().isBlank() ? request.getBaseName() : "documents";
        return buildResponse(entries, request.getFormat(), baseName);
    }

    private Response buildResponse(List<MergedPdfEntry> entries, DownloadFormat format, String baseName) {
        if (format == DownloadFormat.MERGED_PDF) {
            StreamWriter pdf = documentsService.buildMergedPdf(entries);
            if (pdf != null) {
                String filename = URLEncoder.encode(baseName + ".pdf", StandardCharsets.UTF_8)
                        .replace("+", "%20");
                return Response.ok((StreamingOutput) pdf::write)
                        .type("application/pdf")
                        .header("Content-Disposition", "attachment; filename*=utf-8''" + filename)
                        .header("Cache-Control", "no-store")
                        .build();
            }
            // Fall through to ZIP if no PDFs found
            log.info("No PDF attachments found for merged-pdf request — falling back to ZIP");
        }

        List<AttachmentEntity> attachments =
                entries.stream().map(MergedPdfEntry::attachment).toList();
        StreamWriter zip = documentsService.buildZip(attachments, baseName);
        String filename =
                URLEncoder.encode(baseName + ".zip", StandardCharsets.UTF_8).replace("+", "%20");
        return Response.ok((StreamingOutput) zip::write)
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

        eventLogService.log(
                EventType.DOCUMENT_DOWNLOAD, "document", attachment.id(), Map.of("filename", attachment.filename()));

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
            log.atWarn()
                    .setCause(ex)
                    .log(() -> String.format("Failed to build response for document (%s)", docIdentifier));
            return Response.serverError().build();
        }
    }

    @Override
    @RolesAllowed({Roles.MUSIC_LIBRARIAN, Roles.ADMIN})
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
    @RolesAllowed({Roles.MUSIC_LIBRARIAN, Roles.ADMIN})
    public Attachment linkDocument(String docIdentifier, DocumentLinkRequest documentLink) {
        return documentsService.linkDocument(UUID.fromString(docIdentifier), documentLink);
    }

    @Override
    @RolesAllowed({Roles.MUSIC_LIBRARIAN, Roles.ADMIN})
    public SheetClassification classify(String docIdentifier) {
        SheetClassification result = classificationService.classify(UUID.fromString(docIdentifier));
        eventLogService.log(
                EventType.DOCUMENT_CLASSIFIED,
                "document",
                UUID.fromString(docIdentifier),
                Map.of(
                        "title",
                        result.suggested() != null && result.suggested().getTitle() != null
                                ? result.suggested().getTitle()
                                : ""));
        return result;
    }

    @Override
    @RolesAllowed({Roles.MUSIC_LIBRARIAN, Roles.ADMIN})
    public ClassificationApplyResult applyClassification(String docIdentifier, ClassificationApplyRequest request) {
        ClassificationApplyResult result = classificationService.apply(UUID.fromString(docIdentifier), request);
        eventLogService.log(
                EventType.DOCUMENT_CLASSIFICATION_APPLIED,
                "document",
                UUID.fromString(docIdentifier),
                Map.of("sheetId", result.sheetId() != null ? result.sheetId().toString() : ""));
        return result;
    }

    @Override
    @RolesAllowed({Roles.MUSIC_LIBRARIAN, Roles.ADMIN})
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
