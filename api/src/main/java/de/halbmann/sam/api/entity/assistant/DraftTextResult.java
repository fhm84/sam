package de.halbmann.sam.api.entity.assistant;

import lombok.Data;

/** AI-drafted spoken intro text for a programme text item, for the Dirigent to review/edit. */
@Data
public class DraftTextResult {

    String draftText;
}
