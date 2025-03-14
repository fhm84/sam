export interface Instrumentation {

    /**
     * Unique identifier of the instrumentation.
     */
    id: string;
    /**
     * Instrument name (e.g. Trumpet, Violin, Bass)
     */
    instrumentName: string;
    /**
     * The key (for example: 2 for 2. Bass)
     */
    key: number;
    /**
     * Specific key signature for this instrument (e.g., Bb Major, C Major)
     */
    keySignature: string;
    /**
     * Clef type (e.g., Treble, Bass, Alto, Tenor)
     */
    clef: string;

    /**
     * Type of notation (Standard, Tablature, Percussion)
     */
    notationType: string;

}
