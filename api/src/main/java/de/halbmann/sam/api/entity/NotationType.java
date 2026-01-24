package de.halbmann.sam.api.entity;

public enum NotationType {

  /** Standard notation on 5-line musical staves */
  STANDARD,

  /** Guitar */
  TABLATURE,

  /** Drums, Glockenspiel */
  PERCUSSION,

  /** only the melody, lyrics and harmony */
  LEAD_SHEET,

  /**
   * representation of music through the use of visual symbols outside the realm of traditional
   * music notation
   */
  GRAPHIC
}
