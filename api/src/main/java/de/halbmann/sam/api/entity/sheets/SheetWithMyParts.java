package de.halbmann.sam.api.entity.sheets;

import java.util.ArrayList;
import java.util.List;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

/**
 * A sheet music search result augmented with the subset of instrumentations that match the
 * requesting musician's instruments across their ensemble memberships.
 */
@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
public class SheetWithMyParts extends SheetMusicSearchResult {

    List<Instrumentation> myInstrumentations = new ArrayList<>();

    public SheetWithMyParts(SheetMusic sheet, List<Instrumentation> myInstrumentations) {
        super(sheet);
        this.myInstrumentations = myInstrumentations != null ? myInstrumentations : new ArrayList<>();
    }
}
