package de.halbmann.sam.api.entity;

/**
 * Some instruments are constructed in a variety of sizes, with the larger versions having a lower
 * range than the smaller ones. Instruments that transpose this way are often said to be in a
 * certain "key" (e.g., the "B♭ clarinet" or "clarinet in B♭"). This refers to the concert pitch
 * that is heard when a written C is played on the instrument in question. Playing a written C
 * produces a concert B♭ on a B♭ clarinet, a concert A on an A clarinet, and a concert C on a C
 * clarinet (this last example is a non-transposing instrument).
 */
public enum InstrumentTransposing {

    /**
     * C
     */
    C,
    /**
     * D
     */
    D,
    /**
     * E♭
     */
    Eb,
    /**
     * F
     */
    F,
    /**
     * G
     */
    G,
    /**
     * A
     */
    A,
    /**
     * A♭
     */
    Ab,
    /**
     * B♭
     */
    Bb;

    public static InstrumentTransposing fromString(String s) {
        for (InstrumentTransposing instrumentTransposing : InstrumentTransposing.values()) {
            if (instrumentTransposing.name().equalsIgnoreCase(s)) {
                return instrumentTransposing;
            }
        }
        return null;
    }
}
