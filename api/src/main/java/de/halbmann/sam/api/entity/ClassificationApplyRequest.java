package de.halbmann.sam.api.entity;

import java.util.UUID;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request to apply a reviewed classification result: resolves or creates related entities and
 * links the document as an attachment.
 */
@Data
@NoArgsConstructor
public class ClassificationApplyRequest {

    // Sheet: provide existing sheetId OR fill in data to create a new sheet
    UUID sheetId;
    String title;
    String subtitle;
    String publisher;
    Genre genre;
    Integer yearOfComposition;
    String edition;
    String iswc;

    // Composer: provide existing composerId OR name to find/create
    UUID composerId;
    String composerName;

    // Arranger: provide existing arrangerId OR name to find/create
    UUID arrangerId;
    String arrangerName;

    // Instrumentation (optional; when absent, document attaches directly to the sheet)
    String instrumentId;
    String instrumentName;
    String partLabel;
    Clef clef;
    NotationType notationType;

    // Attachment metadata
    AttachmentType attachmentType;
}
