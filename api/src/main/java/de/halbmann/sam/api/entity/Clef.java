package de.halbmann.sam.api.entity;

/**
 * Clefs (de: Notenschlüssel) (musical symbol to indicate which notes are represented by the lines and spaces on a musical staff).
 * <p>
 * https://en.wikipedia.org/wiki/Clef
 * <p>
 * https://en.wikipedia.org/wiki/Clef#/media/File:Common_clefs.svg
 */
public enum Clef {

    /**
     * Treble (de: Violinschüssel) - 𝄞
     */
    TREBLE,

    /**
     * Alto (de: Altschlüssel) - 𝄡
     */
    ALTO,

    /**
     * Tenor (de: Tenorschlüssel) - 𝄡
     */
    TENOR,

    /**
     * Bass (de: Bassschlüssel) - 𝄢
     */
    BASS;

}
