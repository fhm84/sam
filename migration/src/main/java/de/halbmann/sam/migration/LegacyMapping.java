package de.halbmann.sam.migration;

import de.halbmann.sam.api.entity.sheets.Genre;
import de.halbmann.sam.api.entity.sheets.Style;
import java.util.Set;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class LegacyMapping {

    private String legacyGenre;
    private Genre targetGenre;
    private Style targetStyle;
    private Set<String> tags;
}
