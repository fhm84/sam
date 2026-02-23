package de.halbmann.sam.api.entity;

/**
 * Represents the primary musical genre of a sheet music work.
 *
 * <p>
 * In SAM, a {@code Genre} describes the <strong>musical form or category</strong>
 * of a composition (e.g. march, suite, overture), not its stylistic language,
 * intended audience, or usage context.
 * </p>
 *
 * <p>
 * Each {@code SheetMusic} instance MUST be assigned exactly one {@code Genre}.
 * If a work could theoretically fit into multiple categories, the most
 * characteristic or dominant musical form should be chosen.
 * </p>
 *
 * <p>
 * Examples:
 * <ul>
 *   <li>A traditional concert march &rarr; {@code MARCH}</li>
 *   <li>A multi-movement jazz suite &rarr; {@code SUITE}</li>
 *   <li>A film music medley &rarr; {@code FILM_MUSIC}</li>
 * </ul>
 * </p>
 *
 * <p>
 * Stylistic characteristics (e.g. traditional, modern, swing) MUST be modeled
 * using {@link Style}. Situational or contextual aspects (e.g. Christmas,
 * opening concert, youth band) MUST be modeled using tags.
 * </p>
 */
public enum Genre {

    // Märsche & Marschverwandtes
    MARCH, // Konzert- & Straßenmärsche
    MARCHING_SHOW, // Show-/Parademärsche, Field Shows

    // Konzertformen
    CONCERT_WORK, // Allgemeines Konzertwerk
    OVERTURE,
    SUITE,
    SYMPHONY,
    FANTASY,
    VARIATIONS,

    // Tanz & Unterhaltung
    DANCE, // Sammelkategorie (z. B. Foxtrott)
    WALTZ,
    POLKA,

    // Lieder & Chorales
    FOLK_SONG,
    HYMN_CHORALE,

    // Programmatische / thematische Werke
    FILM_MUSIC,
    SHOW_MUSIC,

    // Popular Music
    POP_ROCK,
    JAZZ,

    // Spezielle Kontexte
    LATIN, // Latin als *Form* (z. B. Samba-Suite)
    CHRISTMAS,
    SACRED,

    // Solistische Werke
    SOLO_WITH_BAND
}
