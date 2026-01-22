package de.halbmann.sam.api.entity;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
public class SheetMusicSearchResult extends SheetMusic {

    SearchResultMetrics metrics;

    public SheetMusicSearchResult(SheetMusic sheetMusic) {
        super(sheetMusic.getId(),
                sheetMusic.getTitle(),
                sheetMusic.getSubtitle(),
                sheetMusic.getPublisher(),
                sheetMusic.getPublisherIpi(),
                sheetMusic.getComposer(),
                sheetMusic.getArranger(),
                sheetMusic.getOriginalBy(),
                sheetMusic.getGenre(),
                sheetMusic.getDifficultyLevel(),
                sheetMusic.getYearOfComposition(),
                sheetMusic.getEdition(),
                sheetMusic.getCopyright(),
                sheetMusic.getRating(),
                sheetMusic.getIswc(),
                sheetMusic.getGemaWorkNumber(),
                sheetMusic.getAdditionalNotes(),
                sheetMusic.getInstrumentations(),
                sheetMusic.getAttachments());
    }

    public SheetMusicSearchResult(SheetMusic sheetMusic, SearchResultMetrics metrics) {
        this(sheetMusic);
        this.metrics = metrics;
    }

}
