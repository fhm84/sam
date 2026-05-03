package de.halbmann.sam.api.entity.shares;

import de.halbmann.sam.api.entity.collections.CollectionType;
import de.halbmann.sam.api.entity.documents.Attachment;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import lombok.Data;

/**
 * Public metadata returned for a share token without requiring authentication.
 *
 * <p>The {@code type} field acts as a discriminator: instrumentation-specific fields are populated
 * for {@link ShareType#INSTRUMENTATION} tokens, collection-specific fields for
 * {@link ShareType#COLLECTION} tokens. Fields belonging to the other type are {@code null}.
 *
 * <p>When {@code expired} is {@code true} the token is no longer usable — this covers both
 * past-expiry-date tokens and explicitly revoked ones; callers do not need to distinguish between
 * the two cases.
 */
@Data
public class PublicShareInfo {

    /** The share token UUID, echoed back so the UI can construct download URLs. */
    private UUID tokenId;

    /** Discriminator that determines which group of fields is populated. */
    private ShareType type;

    /** Expiry timestamp, or {@code null} if the link has no expiry. */
    private OffsetDateTime expiresAt;

    /**
     * {@code true} if the token is no longer usable — either because it has passed its expiry
     * date or because it was explicitly revoked by the creator.
     */
    private boolean expired;

    // --- INSTRUMENTATION fields ---

    private String sheetTitle;
    private String composerName;
    private String instrumentName;
    private String partLabel;

    /** Downloadable attachments for this instrumentation (typically one or more PDF files). */
    private List<Attachment> attachments;

    // --- COLLECTION fields ---

    private String collectionName;
    private CollectionType collectionType;

    /** Ordered list of sheet entries in the collection, each with their downloadable parts. */
    private List<PublicShareCollectionItem> sheets;
}
