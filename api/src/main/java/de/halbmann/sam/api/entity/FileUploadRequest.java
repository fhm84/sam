package de.halbmann.sam.api.entity;

import jakarta.ws.rs.FormParam;
import lombok.Getter;
import lombok.Setter;
import org.jboss.resteasy.reactive.multipart.FileUpload;

@Getter
@Setter
public class FileUploadRequest {

    @FormParam("file")
    FileUpload file;

    @FormParam("type")
    AttachmentType type;
}
