package de.halbmann.sam.api.entity.sheets;

import de.halbmann.sam.api.entity.musicians.Musician;
import jakarta.json.bind.annotation.JsonbTypeAdapter;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import java.time.Duration;
import java.util.HashSet;
import java.util.Set;
import lombok.*;

/**
 * Represents a piece of music.
 */
@Data
@EqualsAndHashCode
@NoArgsConstructor
@AllArgsConstructor(access = AccessLevel.PROTECTED)
public class CreateSheetMusic {

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
     * The primary musical genre of this sheet music.
     *
     * <p>
     * The genre represents the dominant musical form or category of the work
     * (e.g. march, suite, overture). Exactly one genre MUST be assigned.
     * </p>
     *
     * <p>
     * The genre is used for:
     * <ul>
     *   <li>primary classification</li>
     *   <li>filtering and search</li>
     *   <li>domain validation rules</li>
     * </ul>
     * </p>
     *
     * <p>
     * Stylistic characteristics MUST be expressed using {@link Style}, and
     * situational aspects MUST be expressed using tags.
     * </p>
     */
    Genre genre;

    /**
     * The stylistic classification of this sheet music.
     *
     * <p>
     * The style describes the musical language or aesthetic of the work and
     * complements the {@link #genre}. This field is optional.
     * </p>
     *
     * <p>
     * Examples:
     * <ul>
     *   <li>A traditional march &rarr; {@code Style.TRADITIONAL}</li>
     *   <li>A contemporary concert work &rarr; {@code Style.CONTEMPORARY}</li>
     *   <li>A jazz-influenced suite &rarr; {@code Style.SWING}</li>
     * </ul>
     * </p>
     *
     * <p>
     * Styles are subject to domain validation rules and may be restricted or
     * considered unusual for certain genres.
     * </p>
     */
    Style style;

    /**
     * Level (Beginner, Intermediate, Advanced).
     */
    @JsonbTypeAdapter(DifficultyJsonbAdapter.class)
    DifficultyLevel difficultyLevel;

    @JsonbTypeAdapter(DurationJsonbAdapter.class)
    Duration duration;

    boolean favorite;

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

    Set<String> tags = new HashSet<>();
}
