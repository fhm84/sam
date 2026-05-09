package de.halbmann.sam.api.entity.collections;

import de.halbmann.sam.api.entity.documents.Attachment;
import de.halbmann.sam.api.entity.sheets.DurationJsonbAdapter;
import de.halbmann.sam.api.entity.sheets.Genre;
import de.halbmann.sam.api.entity.sheets.Style;
import jakarta.json.bind.annotation.JsonbTypeAdapter;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.Duration;
import java.util.UUID;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * An item within a collection — either a sheet reference (SHEET) or a free-text block (TEXT).
 */
@Data
@EqualsAndHashCode
public class CollectionItem {

    UUID id;

    @NotNull
    CollectionItemType type;

    @NotBlank
    String identifier;

    // ── SHEET fields (populated when type = SHEET) ────────────────────────────

    UUID sheetId;
    String title;
    String subtitle;
    Genre genre;
    Style style;

    @JsonbTypeAdapter(DurationJsonbAdapter.class)
    Duration duration;

    // ── TEXT fields (populated when type = TEXT) ──────────────────────────────

    String textContent;

    Attachment attachment;
}
