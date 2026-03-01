package de.halbmann.sam.api.entity;

import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

/**
 * Request object to assign a document to a sheet/instrumentation.
 */
@Getter
@Setter
public class DocumentLinkRequest {

    private UUID sheetId;

    private UUID instrumentationId;

    private AttachmentType attachmentType;
}
