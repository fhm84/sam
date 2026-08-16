package de.halbmann.sam.api.entity.assistant;

import java.util.UUID;
import lombok.Data;

/**
 * A single AI-suggested sheet for a setlist. {@code sheetId} always references a real sheet
 * already in the archive — the assistant is tool-grounded and never invents pieces.
 */
@Data
public class SuggestedSetlistItem {

    UUID sheetId;

    String title;

    String composer;

    /** Why the assistant picked this piece, for the Dirigent to evaluate before adding it. */
    String rationale;
}
