package de.halbmann.sam.api.entity.shares;

import java.util.UUID;
import lombok.Data;

/**
 * One instrumentation entry within a {@link PublicShareCollectionItem}.
 * Carries the display name and a flag indicating whether files are available to download.
 */
@Data
public class PublicShareInstrumentationItem {

    /** Instrumentation primary key, used to build the per-part download URL. */
    private UUID id;

    private String instrumentName;

    /** Optional qualifier distinguishing multiple parts for the same instrument (e.g. "1", "2"). */
    private String partLabel;

    /** {@code true} when at least one attachment file exists for this instrumentation. */
    private boolean hasAttachments;
}
