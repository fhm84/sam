package de.halbmann.sam.api.entity;

/** Types of attachments */
public enum AttachmentType {

  /** Complete conductor’s score */
  FULL_SCORE,
  /** Individual instrument parts (e.g., Trumpet 1) */
  PART,
  /** Title page or decorative cover */
  COVER,
  /** Lyrics only (could be PDF, TXT, DOCX) */
  LYRICS,
  /** MIDI file for playback or practicing */
  MIDI,
  /** MP3 or WAV recording of the piece */
  AUDIO,
  /** Notes, instructions, fingerings, alternate versions */
  ANNOTATIONS,
  /** Scanned image (JPG, PNG) of any kind */
  IMAGE,
  /** Harmonic/formal analysis PDF, theory notes */
  ANALYSIS,
  /** Manually transcribed version */
  TRANSCRIPTION,
  /** URL to an external source (YouTube, IMSLP, etc.) */
  EXTERNAL_LINK,
  /** MusicXML - the standard open format for exchanging digital sheet music */
  MUSIC_XML,
  /** Any other type of attachment */
  OTHER,
  /** Unspecified attachment (like e.g. non-classified documents) */
  UNSPECIFIED
}
