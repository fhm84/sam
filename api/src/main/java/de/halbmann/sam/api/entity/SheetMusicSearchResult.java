package de.halbmann.sam.api.entity;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
public class SheetMusicSearchResult extends SheetMusic {

    double ftsRank;
    double titleSimilarity;
    double composerSimilarity;
    boolean phoneticMatch;
    double finalRank;

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

    public SheetMusicSearchResult(SheetMusic sheetMusic,
                                  double ftsRank,
                                  double titleSimilarity,
                                  double composerSimilarity,
                                  boolean phoneticMatch,
                                  double finalRank) {
        this(sheetMusic);
        this.ftsRank = ftsRank;
        this.titleSimilarity = titleSimilarity;
        this.composerSimilarity = composerSimilarity;
        this.phoneticMatch = phoneticMatch;
        this.finalRank = finalRank;
    }

}
