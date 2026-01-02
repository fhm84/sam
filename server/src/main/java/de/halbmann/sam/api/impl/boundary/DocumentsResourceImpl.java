package de.halbmann.sam.api.impl.boundary;

import de.halbmann.sam.api.boundary.DocumentsResource;
import de.halbmann.sam.api.entity.Attachment;
import de.halbmann.sam.api.entity.DocumentDownload;
import de.halbmann.sam.api.entity.DocumentFilterRequest;
import de.halbmann.sam.api.entity.PaginatedResponse;
import de.halbmann.sam.business.boundary.DocumentsService;
import de.halbmann.sam.business.entity.DocumentEntity;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.StreamingOutput;
import lombok.extern.slf4j.Slf4j;
import org.jboss.resteasy.reactive.multipart.FileUpload;
import org.slf4j.event.Level;

import java.io.IOException;
import java.io.InputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.security.NoSuchAlgorithmException;
import java.util.List;

@Slf4j
@RequestScoped
public class DocumentsResourceImpl implements DocumentsResource {

    @Inject
    DocumentsService documentsService;

    @PathParam("instrumentationId")
    String instrumentationId;

    @Override
    public PaginatedResponse<DocumentDownload> list(DocumentFilterRequest filterRequest) {
        // TODO: maybe we could/should also implement the filter to not only show unlinked documents?
        List<DocumentEntity> unlinkedDocuments = documentsService.listUnlinkedDocuments();
        PaginatedResponse<DocumentDownload> response = new PaginatedResponse<>();
        response.setData(unlinkedDocuments.stream()
                .map(d -> new DocumentDownload(null, d.getId(), d.getFilename(), d.getSize(), d.getMimeType(), d.getSha256()))
                .toList()
        );
        response.setSize(response.getData().size());
        response.setTotalCount(response.getData().size());
        return response;
    }

    @Override
    public Response load(String docIdentifier) {
        DocumentDownload attachment;
        try {
            attachment = documentsService.loadAttachment(instrumentationId, docIdentifier);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        if (attachment == null) {
            log.info(String.format("Document (%s) not found", docIdentifier));
            return Response.status(Response.Status.NOT_FOUND).build();
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
                    .header("Content-Disposition",
                            "inline; filename*=utf-8''" + encodedFilename.replaceAll("\\+", "%20"))
                    .build();
        } catch (final Exception ex) {
            log.atInfo().setCause(ex).log(() -> String.format("Loading document (%s) resulted in", docIdentifier));
            return Response.noContent().build();
        }
    }

    @Override
    public String uploadDocument(FileUpload file) {
        log.atLevel(Level.INFO).log(() -> "File-Upload ... filename: " + file.fileName());

        // TODO: if instrumentationId is set, we have to directly add/link it!!!
        try (InputStream inputStream = Files.newInputStream(file.uploadedFile())) {
            Attachment attachment = documentsService.save(file.fileName(), inputStream);
            return "{\"fileName\":\"" + attachment.getDisplayName() + "\", \"id\":\"" + attachment.getId() + "\"}";
        } catch (IOException | NoSuchAlgorithmException e) {
            log.atWarn().setCause(e).log(() -> "Failed to upload file " + file.fileName());
            return "{\"error\":\"Failed to save file\"}";
        }
    }

}
