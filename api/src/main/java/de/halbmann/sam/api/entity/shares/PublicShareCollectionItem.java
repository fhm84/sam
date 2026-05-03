package de.halbmann.sam.api.entity.shares;

import java.util.List;
import java.util.UUID;
import lombok.Data;

/**
 * One sheet entry within a {@link PublicShareInfo} of type {@link ShareType#COLLECTION}.
 * Contains the sheet's basic metadata and the list of its instrumentations that have
 * downloadable attachments.
 */
@Data
public class PublicShareCollectionItem {

    private UUID sheetId;
    private String title;
    private String subtitle;
    private String composerName;

    /** All instrumentations of this sheet; only those with {@code hasAttachments = true} offer a download. */
    private List<PublicShareInstrumentationItem> instrumentations;
}
