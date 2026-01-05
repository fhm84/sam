package de.halbmann.sam.api.entity;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Represents a piece of music.
 */
@Data
@EqualsAndHashCode
@NoArgsConstructor
@AllArgsConstructor(access = AccessLevel.PROTECTED)
public class SheetMusic {

    /**
     * Unique identifier of the music sheet/piece.
     */
    UUID id;
    /**
     * The title of the music sheet/piece.
     */
    @NotBlank
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
     * Interested Party Information (IPI)-Number of the publisher.
     */
    String publisherIpi;
    /**
     * The composer of the music sheet.
     */
    @Valid
    Musician composer;
    /**
     * The arranger of the music sheet.
     */
    @Valid
    Musician arranger;

    /**
     * Optional (e.g., original band or composer)
     */
    String originalBy;

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
     * Copyright information.
     */
    String copyright;

    /**
     * Rating for the piece/music sheet.
     */
    Integer rating;

    /**
     * International Standard Musical Work Code
     */
    String iswc;

    /**
     * Identification number of GEMA (GEMA-Werk Nr.)
     */
    String gemaWorkNumber;

    /**
     * Additional notes.
     */
    String additionalNotes;

    /**
     * Individual instrument parts for a piece of sheet music.
     */
    @Valid
    List<Instrumentation> instrumentations = new ArrayList<>();

    /**
     * Metadata of the sheet music files (including location, mime-type, fileSize, ...)
     */
    Set<Attachment> attachments;

}
