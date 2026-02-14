package de.halbmann.sam.api.impl.boundary;

import de.halbmann.sam.api.boundary.DocumentsResource;
import de.halbmann.sam.api.entity.*;
import de.halbmann.sam.business.boundary.DocumentsService;
import de.halbmann.sam.business.entity.DocumentEntity;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.StreamingOutput;
import java.io.IOException;
import java.io.InputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.event.Level;

@Slf4j
@RequestScoped
public class DocumentsResourceImpl implements DocumentsResource {

    @Inject
    DocumentsService documentsService;

    @PathParam("instrumentationId")
    String instrumentationId;

    @PathParam("sheetId")
    String sheetId;

    @Override
    public PaginatedResponse<DocumentDownload> list(DocumentFilterRequest filterRequest) {
        // TODO: maybe we could/should also implement the filter to not only show unlinked documents?
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
    public Response load(String docIdentifier, String ifNoneMatch) {
        DocumentDownload attachment;
        if (instrumentationId != null) {
            try {
                attachment = documentsService.loadAttachmentByInstrumentation(instrumentationId, docIdentifier);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        } else if (sheetId != null) {
            try {
                attachment = documentsService.loadAttachmentBySheet(sheetId, docIdentifier);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        } else {
            try {
                attachment = documentsService.loadAttachment(docIdentifier);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
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
    public String uploadDocument(FileUploadRequest request) {
        log.atLevel(Level.INFO)
                .log(() -> "File-Upload ... filename: " + request.getFile().fileName());

        // TODO: if instrumentationId or sheetId is set, we have to directly add/link it!!!
        try (InputStream inputStream = Files.newInputStream(request.getFile().uploadedFile())) {
            Attachment attachment = documentsService.save(request.getFile().fileName(), inputStream, request.getType());
            return """
                    "filename": "%s",
                    "id": "%s"
                    """.formatted(attachment.getDisplayName(), attachment.getId());
        } catch (IOException | NoSuchAlgorithmException e) {
            log.atWarn()
                    .setCause(e)
                    .log(() -> "Failed to upload file " + request.getFile().fileName());
            return """
                    "error": "Failed to save file"
                    """;
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
