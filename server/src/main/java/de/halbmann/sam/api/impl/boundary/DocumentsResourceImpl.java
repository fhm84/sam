package de.halbmann.sam.api.impl.boundary;

import de.halbmann.sam.api.boundary.DocumentsResource;
import de.halbmann.sam.api.entity.Attachment;
import de.halbmann.sam.business.boundary.DocumentsService;
import de.halbmann.sam.business.entity.AttachmentWrapper;
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

@Slf4j
@RequestScoped
public class DocumentsResourceImpl implements DocumentsResource {

    @Inject
    DocumentsService documentsService;

    @PathParam("instrumentationId")
    String instrumentationId;

    @Override
    public Response load(String docIdentifier) {
        AttachmentWrapper attachment;
        try {
            attachment = documentsService.loadAttachment(instrumentationId, docIdentifier);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        if (attachment == null) {
            log.info(String.format("Document (%s) not found", docIdentifier));
            return Response.status(Response.Status.NOT_FOUND).build();
        }

        try (InputStream loaded = attachment.dataStream()) {
            final StreamingOutput streamingOutput = loaded::transferTo;
            final String encodedFilename = URLEncoder.encode(attachment.attachment().getDisplayName(), StandardCharsets.UTF_8);
            return Response.ok(streamingOutput)
                    .type(attachment.attachment().getMimeType())
                    .header("Content-Disposition",
                            "inline; filename*=utf-8''" + encodedFilename.replaceAll("\\+", "%20"))
                    .build();
        } catch (final Exception ex) {
            log.info(String.format("Loading document (%s) resulted in", docIdentifier), ex);
            return Response.noContent().build();
        }
    }

    @Override
    public String uploadDocument(FileUpload file) {
        log.atLevel(Level.INFO).log(() -> "File-Upload ... filename: " + file.fileName());

        try (InputStream inputStream = Files.newInputStream(file.uploadedFile())) {
            Attachment attachment = documentsService.save(file.fileName(), inputStream);
            return "{\"fileName\":\"" + attachment.getDisplayName() + "\", \"documentIdentifier\":\"" + attachment.getDocIdentifier() + "\"}";
        } catch (IOException e) {
            return "{\"error\":\"Failed to save file\"}";
        }
    }

}
