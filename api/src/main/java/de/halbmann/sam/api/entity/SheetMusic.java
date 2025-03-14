package de.halbmann.sam.api.entity;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Represents a piece of music.
 */
@Data
@EqualsAndHashCode
public class SheetMusic {

    /**
     * Unique identifier of the music sheet/piece.
     */
    UUID id;
    /**
     * The title of the music sheet/piece.
     */
    String title;
    /**
     * (Optional) Subtitle of the piece.
     */
    String subtitle;
    /**
     * The publisher of the music sheet.
     */
    String publisher;
    /**
     * The composer of the music sheet.
     */
    Musician composer;
    /**
     * The arranger of the music sheet.
     */
    Musician arranger;

    /**
     * Classification (e.g., Classical, Jazz)
     */
    String genre;

    /**
     * Level (Beginner, Intermediate, Advanced).
     */
    String difficultyLevel;

    /**
     * Year of composition.
     */
    Integer yearOfComposition;

    /**
     * Edition name.
     */
    String edition;

    /**
     * License information.
     */
    String license;

    /**
     * Rating for the piece/music sheet.
     */
    Integer rating;

    /**
     * Additional notes.
     */
    String additionalNotes;

    List<Instrumentation> instrumentations = new ArrayList<>();

}
