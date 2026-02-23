package de.halbmann.sam.api.entity;

/**
 * Represents the stylistic or aesthetic characteristics of a musical work.
 *
 * <p>
 * A {@code Style} describes <strong>how</strong> a piece of music sounds or is
 * written (e.g. classical, modern, swing), independent of its musical form
 * or genre.
 * </p>
 *
 * <p>
 * The {@code Style} attribute is optional and intended as a complementary
 * classification to {@link Genre}. Not all works require a style assignment,
 * especially if the style is implicit in the genre.
 * </p>
 *
 * <p>
 * Examples:
 * <ul>
 *   <li>{@code Genre.MARCH} + {@code Style.TRADITIONAL}</li>
 *   <li>{@code Genre.SUITE} + {@code Style.SWING}</li>
 *   <li>{@code Genre.CONCERT_WORK} + {@code Style.CONTEMPORARY}</li>
 * </ul>
 * </p>
 *
 * <p>
 * Contextual information such as target audience, performance occasion, or
 * difficulty level MUST NOT be modeled as a {@code Style}.
 * </p>
 */
public enum Style {

    // Epochen & Grundästhetik
    CLASSICAL,
    ROMANTIC,
    MODERN,
    CONTEMPORARY,

    // Popular & Groove
    POP,
    ROCK,
    FUNK,
    SWING,
    LATIN,

    // Tradition & Herkunft
    TRADITIONAL,
    FOLKLORISTIC,

    // Sonstiges
    EXPERIMENTAL
}
