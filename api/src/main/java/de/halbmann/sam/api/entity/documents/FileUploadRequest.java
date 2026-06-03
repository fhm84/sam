package de.halbmann.sam.api.entity.documents;

import jakarta.ws.rs.FormParam;
import lombok.Getter;
import lombok.Setter;
import org.jboss.resteasy.reactive.multipart.FileUpload;

/** Multipart form data received on document upload endpoints. */
@Getter
@Setter
public class FileUploadRequest {

    @FormParam("file")
    FileUpload file;

    @FormParam("type")
    AttachmentType type;
}
