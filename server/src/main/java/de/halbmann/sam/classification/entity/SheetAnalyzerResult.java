package de.halbmann.sam.classification.entity;

import de.halbmann.sam.api.entity.Clef;
import de.halbmann.sam.api.entity.NotationType;

/**
 * @param title The title of the music sheet/piece.
 * @param subtitle (Optional) Subtitle of the piece.
 * @param publisher The publisher of the music sheet.
 * @param composer The composer of the music sheet.
 * @param arranger The arranger of the music sheet.
 * @param genre Classification (e.g., Classical, Jazz)
 * @param yearOfComposition Year of composition.
 * @param edition Edition name.
 * @param iswc International Standard Musical Work Code
 */
public record SheetAnalyzerResult(
    String title,
    String subtitle,
    String publisher,
    String composer,
    String arranger,
    String genre,
    Integer yearOfComposition,
    String edition,
    String iswc,
    InstrumentationAnalyzerResult instrumentation) {

  /**
   * @param instrumentName Instrument name (e.g. Trumpet, Violin, Bass)
   * @param partLabel The part label (for example: '2. Bass in C', or 'Solo Trumpet in Bb')
   * @param partNumber The part number/label (for example: '2' for 2. Bass, or even '3rd' or 'Solo')
   * @param transposition Specific key signature for this instrument (e.g., Bb Major, C Major)
   * @param clef Clef type (e.g., Treble, Bass, Alto, Tenor)
   * @param notationType Type of notation (Standard, Tablature, Percussion)
   */
  public record InstrumentationAnalyzerResult(
      String instrumentName,
      String partLabel,
      String partNumber,
      String transposition,
      Clef clef,
      NotationType notationType) {}
}
