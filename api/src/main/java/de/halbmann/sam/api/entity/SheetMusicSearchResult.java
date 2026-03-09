package de.halbmann.sam.api.entity;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
public class SheetMusicSearchResult extends SheetMusic {

    SearchResultMetrics metrics;

    CoverageSnapshotSummary coverage;

    public SheetMusicSearchResult(SheetMusic sheetMusic) {
        super(
                sheetMusic.getId(),
                sheetMusic.getTitle(),
                sheetMusic.getSubtitle(),
                sheetMusic.getPublisher(),
                sheetMusic.getPublisherIpi(),
                sheetMusic.getComposer(),
                sheetMusic.getArranger(),
                sheetMusic.getOriginalBy(),
                sheetMusic.getGenre(),
                sheetMusic.getStyle(),
                sheetMusic.getDifficultyLevel(),
                sheetMusic.getDuration(),
                sheetMusic.isFavorite(),
                sheetMusic.getYearOfComposition(),
                sheetMusic.getEdition(),
                sheetMusic.getCopyright(),
                sheetMusic.getRating(),
                sheetMusic.getIswc(),
                sheetMusic.getGemaWorkNumber(),
                sheetMusic.getAdditionalNotes(),
                sheetMusic.getTags(),
                sheetMusic.getInstrumentations(),
                sheetMusic.getAttachments());
    }

    public SheetMusicSearchResult(SheetMusic sheetMusic, SearchResultMetrics metrics) {
        this(sheetMusic);
        this.metrics = metrics;
    }
}
