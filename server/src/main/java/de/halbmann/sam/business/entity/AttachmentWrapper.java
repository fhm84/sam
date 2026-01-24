package de.halbmann.sam.business.entity;

import de.halbmann.sam.api.entity.Attachment;
import java.io.InputStream;

public record AttachmentWrapper(Attachment attachment, InputStream dataStream) {}
