package de.halbmann.sam.business.documents.entity;

import de.halbmann.sam.api.entity.documents.Attachment;
import java.io.InputStream;

public record AttachmentWrapper(Attachment attachment, InputStream dataStream) {}
